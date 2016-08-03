/*
 *******************************************************************************
 *
 * vdm_swmc_ecu_prot_pl.c
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

#include <vdm_components.h>
#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_error.h>
#include <vdm_pl_alloc.h>
#include <vdm_utl_logger.h>
#include <vdm_utl_str.h>
#include <vdm_error.h>
#include <vdm_swmc_ecu_prot_pl.h>
#include <vdm_pl_alloc.h>
#include <vdm_pl_memory.h>
#include <vdm_pl_stdlib.h>
#include <vdm_pl_string_utils.h>
#include <vdm_client_pl_storage.h>
#include <vdm_utl_cfgparser.h>
#include <vdm_pl_string.h>
#include <unistd.h>
#include <vdm_swmc_util.h>

#define VDM_COMPONENT_ID    (E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC

#define ECU_COMP_ID (const char *)"FUSE"
#define ECU_SOURCE_ROM_PATH ECU_COMP_ID "_SOURCE"

#define ECU_COMP_FILE_NAME "ecu_components.txt"

#define ECU_HW_FILE_NAME "ecu_hw_components.txt"
#define ECU_ATTRIBUTE_STRING_ADDRESS "address"
#define ECU_ATTRIBUTE_STRING_SERIAL "serial"
#define ECU_ATTRIBUTE_STRING_PART "part"
#define ECU_ATTRIBUTE_STRING_UNIT "unit"
#define ECU_ATTRUBUTE_FILE_POSTFIX ".txt"

#define ECU_SLEEP_SIMULATE_FILE "sleep.txt"
#define ECU_SLEEP_SIMULATE_INIT "init"
#define ECU_SLEEP_SIMULATE_HW_SYNC "hw_sync"
#define ECU_SLEEP_SIMULATE_SW_SYNC "sw_sync"

#define ECU_COMP_FILE_PLAN_SUFFIX "plan"
#define ECU_COMP_FILE_PLAN_PRE_SUFFIX "PreInstall"
#define ECU_COMP_FILE_PLAN_POST_SUFFIX "PostInstall"
#define ECU_COMP_FILE_MAX_LINE 256
#define MAX_PATH 1024

#define ECU_COMP_DEFAULT_VERSION "1.0.0.0"
// for the moment to get response
SWM_PROT_TLV_Command_t s_currentCmd;
// create internal command enum
typedef enum {
	E_FILE_Command_error,
	E_FILE_Command_sleep,
	E_FILE_Command_progress,
	E_FILE_Command_updateResult
} E_FILE_Command_t;

#define DOLLAR_SEPERATOR '$'
#define EQUAL_SEPERATOR '='
///////////////////////////////////////////////////////////////////
//////////////// STATIC FUNCTIONS /////////////////////////////////
///////////////////////////////////////////////////////////////////

static void addNullToEOL(IU8 *inString)
{
	char *pEOL = NULL;

	pEOL = VDM_PL_strchr((const char *)inString, '\n');
	if (pEOL)//add null terminated instead of EOL
	{
		*pEOL = '\0';
	}
	return;
}

static char *returnStringTag(E_SWM_PROT_Tag_t inTag)
{
	char *pStringTag = NULL;

	switch (inTag)
	{
	case E_SWM_PROT_Tag_init:
	{
		pStringTag = (char *)ECU_SLEEP_SIMULATE_INIT;
		break;
	}
	case E_SWM_PROT_Tag_getNodeAddress:
	{
		pStringTag = (char *)ECU_SLEEP_SIMULATE_HW_SYNC;
		break;
	}
	case E_SWM_PROT_Tag_getCompId:
	{
		pStringTag = (char *)ECU_SLEEP_SIMULATE_SW_SYNC;
		break;
	}
	default:
		break;
	}
	return pStringTag;
}

/* we want to allow sleep for testing for 3 operations
 * 1) init process
 * 2) get AddressNode (first HW sync)
 * 3) get AddressNode (first HW sync)*/
static VDM_Error sleepSimulate(E_SWM_PROT_Tag_t inTag)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;
	char *stringTag = NULL;
	VDM_Error ret = VDM_ERR_OK;

	if (inTag == E_SWM_PROT_Tag_getCompId ||
	    inTag == E_SWM_PROT_Tag_getNodeAddress ||
	    inTag == E_SWM_PROT_Tag_init)
	{
		result = VDM_Client_PL_Storage_openByName(&handle, (char *)ECU_SLEEP_SIMULATE_FILE, E_VDM_CLIENT_PL_Storage_Access_read);
		if (!handle || result != VDM_ERR_OK)
		{
			goto end;
		}
		stringTag = returnStringTag(inTag);
		do
		{
			if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
			{
				VDM_logError(("finished\error reading file  %s'", ECU_COMP_FILE_NAME));
				goto end;
			}

			addNullToEOL(buf);

			result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, '=', &key, &value);
			if (value != NULL && key != NULL)// the same ID, need to get next one
			{
				if (VDM_PL_strcmp((const char *)key, (const char *)stringTag) == 0)
				{
					if (value && (*value != '-')) // if -[num] is set we return error (example: -1)
					{
						IBOOL convertStr = FALSE;
						int sleepTime = (int)VDM_PL_atoIU32((const char * )value, (IU8)10, &convertStr);

						if (convertStr)
						{
							VDM_logDebug(("sleep request was found for key: %s for time %s", key, value));
							sleep((unsigned int)sleepTime);
							VDM_logDebug(("sleep request for %s ended after %s time", key, value));
							goto end;
						}
						else
						{
							VDM_logDebug(("sleep request was found but convert of %s has failed", value));
						}
					}
					else
					{
						ret = VDM_ERR_NOT_INITILIZED;
						VDM_logDebug(("sleep request requested failure %d", ret));
						goto end;
					}
					break; // found our component ID
				}
			}
		} while (1);
	}

end:
	if (handle)
	{
		VDM_Client_PL_Storage_close(handle, FALSE);
	}

	return ret;
}

static void setDupStringToCmdStringData(E_SWM_PROT_DataType_t *outDataType,
	SWM_PROT_TLV_Str_t  *outStrData,    char *inStringData)
{
	outStrData->string = (const unsigned char *)VDM_UTL_strdup((const char *)inStringData);
	outStrData->length = (unsigned int)VDM_PL_strlen((const char *)inStringData);
	*outDataType = E_SWM_PROT_DataType_str;
}

static void updateNewCompVersionInFile(int inUpdateResult)
{
	IU8 searchCompId[ECU_COMP_FILE_MAX_LINE + 1] = {'\0'};
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;
	char *ecuCompFile = ECU_COMP_FILE_NAME;
	IU8 fileInRam[MAX_PATH] = {'\0'};
	IU32 loadedFileSize = 0;
	IU32 loadedFileReadSize = 0;
	char *pCompIdInFile = NULL;

	/* TODO - what if new installed component?
	 * what about updating component name?
	 */

	if (inUpdateResult)// if not 0 , then update failed
	{
		VDM_logDebug(("Update failed, no need to update new version to %s", ecuCompFile));
		goto end;
	}

	result = VDM_Client_PL_Storage_loadFile(ecuCompFile, fileInRam, MAX_PATH,
		&loadedFileReadSize, &loadedFileSize);
	if (result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot load file '%s' for writing", ecuCompFile));
		goto end;
	}

	// comp id to find
	VDM_PL_snprintf((char *)searchCompId, ECU_COMP_FILE_MAX_LINE, "%s=", s_currentCmd.comp_id.string);

	pCompIdInFile = VDM_PL_strstr((const char *)fileInRam, (const char *)searchCompId);
	if (!pCompIdInFile)
	{
		VDM_logError(("Cannot not find Component Id %s in file %s",
			s_currentCmd.comp_id.string, ecuCompFile));
		goto end;
	}

	pCompIdInFile += VDM_PL_strlen((const char *)searchCompId);
	*pCompIdInFile = (char)((int)*pCompIdInFile + 1);

	// over write the file
	result = VDM_Client_PL_Storage_openByName(&handle,
		ecuCompFile, E_VDM_CLIENT_PL_Storage_Access_write);
	if (!handle || result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot open file '%s' for writing", ecuCompFile));
		goto end;
	}

	// means that we need to rewrite this line
	result = VDM_Client_PL_Storage_write(handle, (void *)fileInRam, loadedFileSize);
	if (result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot write to file '%s' ", ecuCompFile));
	}

	VDM_Client_PL_Storage_close(handle, (result == VDM_ERR_OK) ? TRUE : FALSE);
end:

	return;
}

updateSimContext_t *s_updateSimContext = NULL;

static E_SWM_PROT_Tag_t getFileResponse(int *outResponseValue)
{
	VDM_Error result;
	E_SWM_PROT_Tag_t ret;

	ret = E_SWM_PROT_Tag_updateResult;
	*outResponseValue =  -1;
	if (!s_updateSimContext)
	{
		goto end;
	}

	while (1)
	{
		E_Update_Sim_Cmd_t responseType;

		result = getNextUpdateSimCmd(s_updateSimContext,
			&responseType, outResponseValue);
		if (result != VDM_ERR_OK)
		{
			break;
		}

		switch (responseType)
		{
		case E_Update_Sim_updateResult:
			VDM_logDebug(("File Response returned update reulst of %d ", *outResponseValue));
			ret = E_SWM_PROT_Tag_updateResult;
			// Update ecu_components file with the new version if update finished with success
			updateNewCompVersionInFile(*outResponseValue);
			goto end; // update finished
		case E_Update_Sim_progress:
			VDM_logDebug(("File Response returned update progress of %d ", *outResponseValue));
			ret = E_SWM_PROT_Tag_progress;
			goto end; // we want to save the handle open till end of command lines
		case E_Update_Sim_sleep:
			// we will sleep and continue to next line
			VDM_logDebug(("File Response returned sleep request for %d ", *outResponseValue));
			sleep((unsigned int)*outResponseValue);
			continue; // to next line read
		default:
			VDM_logDebug(("File Response returned unknown command we send update result fail!!!"));
			ret = E_SWM_PROT_Tag_updateResult;
			*outResponseValue =  -1;
			goto end;
		}
	}

end:
	return ret;
}

static void generatePlanFileName(E_SWM_PROT_InsPhase_t inInsPhase,
	const char *inDeltaPath, char *outPlanFileName)
{
	char phase[MAX_PATH] = {'\0'};
	char *pCompIdName = NULL;
	char seperater = '.';

	pCompIdName = VDM_PL_strchr((const char *)inDeltaPath, seperater);
	if (pCompIdName == NULL) // something is wrong, we fail
	{
		VDM_logDebug(("could not get char %c from file %s, we fail the update", seperater, inDeltaPath));
		return;
	}
	VDM_logDebug(("Install phase is: %d delta path is: %s", inInsPhase,
		VDM_UTL_stringPrintNull(inDeltaPath)));
	*pCompIdName = '\0'; // set NULL after component name

	if (inInsPhase == E_SWM_INS_PHASE_PRE_INSTALL)
	{
		VDM_PL_snprintf(phase, MAX_PATH, "_%s", ECU_COMP_FILE_PLAN_PRE_SUFFIX);
	}
	if (inInsPhase == E_SWM_INS_PHASE_POST_INSTALL)
	{
		VDM_PL_snprintf(phase, MAX_PATH, "_%s", ECU_COMP_FILE_PLAN_POST_SUFFIX);
	}

	VDM_PL_snprintf(outPlanFileName, MAX_PATH, "%s%s.%s", inDeltaPath, phase, ECU_COMP_FILE_PLAN_SUFFIX);

	VDM_logDebug(("Generated plan file name is: %s",
		VDM_UTL_stringPrintNull(outPlanFileName)));
}

static void updateEcuPL(SWM_PROT_TLV_Command_t *outCmd)
{
	int fileResponseRetValue = 0;

	outCmd->data_type = E_SWM_PROT_DataType_num;

	switch (getFileResponse(&fileResponseRetValue))
	{
	case E_SWM_PROT_Tag_progress:
	{
		outCmd->data.num.number = fileResponseRetValue;
		outCmd->tag = E_SWM_PROT_Tag_progress;
		break;
	}
	case E_SWM_PROT_Tag_updateResult:
	{
		outCmd->data.num.number = fileResponseRetValue;
		outCmd->tag = E_SWM_PROT_Tag_updateResult;
		break;
	}
	default:
		break;
	}
}

// read from ecu_components.txt
static VDM_Error findCompAttr(IU8 *inCompId, char **outBuf, IU32 *outBufLength, IU8 inSeparator)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error ret = VDM_ERR_OK;
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;

	result = VDM_Client_PL_Storage_openByName(&handle, (char *)ECU_COMP_FILE_NAME, E_VDM_CLIENT_PL_Storage_Access_read);
	if (!handle || result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot open file '%s' for reading", ECU_COMP_FILE_NAME));
		goto end;
	}

	while (VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
	{
		// Separator is '=' for component version lookup, '$' for component name lookup
		result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, inSeparator, &key, &value);
		if (value != NULL && key != NULL)
		{
			if (VDM_PL_strcmp((const char *)key, (const char *)inCompId) == 0)
			{
				/* handle special case that we have EOL \n in the end */
				addNullToEOL(value);
				*outBufLength = VDM_PL_strlen((const char *)value);
				*outBuf = VDM_UTL_strndup((const char *)value, *outBufLength);
				break; // found our component ID
			}
		}
		// initialize buffer before next read
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);
	}

end:
	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);

	return ret;
}

static VDM_Error getCompVersion(IU8 *inCompId, char **outVersion, IU32 *outVersionLength)
{
	return findCompAttr(inCompId, outVersion, outVersionLength, EQUAL_SEPERATOR);
}

static VDM_Error getCompName(IU8 *inCompId, char **outName, IU32 *outNameLength)
{
	return findCompAttr(inCompId, outName, outNameLength, DOLLAR_SEPERATOR);
}

static VDM_Error findStringInFile(const unsigned char *inEcuId, char *inEcuAttribute, char *outData)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 fullFileName[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error ret = VDM_ERR_OK;
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;

	// create file string:
	VDM_PL_snprintf((char *)fullFileName, ECU_COMP_FILE_MAX_LINE, "%s%s", inEcuId, ECU_ATTRUBUTE_FILE_POSTFIX);

	result = VDM_Client_PL_Storage_openByName(&handle, (char *)fullFileName, E_VDM_CLIENT_PL_Storage_Access_read);
	if (!handle || result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot open file '%s' for reading", fullFileName));
		goto end;
	}

	do
	{
		if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
		{
			VDM_logError(("finished reading from file '%s'", fullFileName));
			goto end;
		}

		addNullToEOL(buf);

		result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, '=', &key, &value);
		if (key != NULL)
		{
			// we try to find the entry
			if (VDM_PL_strstr((const char *)key, (const char *)inEcuAttribute))
			{
				// means we do not have attribute, we return 0
				if (value)
				{
					VDM_PL_strlcpy((char *)outData, (const char *)value, VDM_PL_strlen((const char *)value) + 1);
				}
				// data was found
				break;
			}
		}
		// initialize buffer before next read
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);
	} while (result == VDM_ERR_OK);

end:
	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);

	return ret;
}

static VDM_Error getEcuHwAttribute(const unsigned char *inEcuId, E_SWM_PROT_Tag_t inEcuAttribute, char *outData)
{
	VDM_Error ret = VDM_ERR_OK;

	// if there is no ECU ID, we will not be able to open the file.
	if (!inEcuId)
	{
		goto end;
	}

	switch (inEcuAttribute)
	{
	case E_SWM_PROT_Tag_getSerialNumber:
	{
		ret = findStringInFile(inEcuId, ECU_ATTRIBUTE_STRING_SERIAL, outData);
		break;
	}
	case E_SWM_PROT_Tag_getPartNumber:
	{
		ret = findStringInFile(inEcuId, ECU_ATTRIBUTE_STRING_PART, outData);
		break;
	}
	case E_SWM_PROT_Tag_getManagementUnit:
	{
		ret = findStringInFile(inEcuId, ECU_ATTRIBUTE_STRING_UNIT, outData);
		break;
	}
	case E_SWM_PROT_Tag_getNodeAddress:
	{
		ret = findStringInFile(inEcuId, ECU_ATTRIBUTE_STRING_ADDRESS, outData);
		break;
	}
	default:
		break;
	}
end:
	return ret;
}

static VDM_Error getCompIdInfo(E_SWM_PROT_Tag_t inTag, IU8 *inCompId, char **outCompId, IU32 *outCompIdLength)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error ret = VDM_ERR_OK;
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;
	IBOOL currCompIdUse = FALSE;
	char *fileName = (inTag == E_SWM_PROT_Tag_getEcuId) ? (char *)ECU_HW_FILE_NAME : (char *)ECU_COMP_FILE_NAME;

	result = VDM_Client_PL_Storage_openByName(&handle,
		fileName, E_VDM_CLIENT_PL_Storage_Access_read);
	if (!handle || result != VDM_ERR_OK)
	{
		ret = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	do
	{
		if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
		{
			VDM_logDebug(("finished reading lines from '%s'", fileName));
			goto end;
		}

		// Handle only if EQUAL_SEPERATOR
		if (VDM_PL_strchr((const char *)buf, EQUAL_SEPERATOR))
		{
            result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, EQUAL_SEPERATOR, &key, &value);
            if (key != NULL)// the same ID, need to get next one
            {
                if (inCompId == NULL)
                {
                    // if no prev we use current component ID
                    currCompIdUse = TRUE;
                }

                if (currCompIdUse == TRUE)
                {
                    *outCompIdLength = VDM_PL_strlen((const char *)key);
                    *outCompId = VDM_UTL_strndup((const char *)key, *outCompIdLength);
                    break; // found our component ID
                }

                if (VDM_PL_strcmp((const char *)key, (const char *)inCompId) == 0) // need to find prev and then take next compId
                {
                    // need to allocate prev ECU ID and then use the next one.
                    currCompIdUse = TRUE;
                }
            }
		}
		// initialize buffer before next read
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);
	} while (result == VDM_ERR_OK);

end:

	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);

	if (result != VDM_ERR_OK)
	{
		ret = VDM_ERR_UNSPECIFIC;
	}

	return ret;
}

static void releaseCmd(SWM_PROT_TLV_Command_t *inCmd)
{
	VDM_PL_freeAndNullify(inCmd->comp_id.string);
	inCmd->comp_id.length = 0;
	VDM_PL_freeAndNullify(inCmd->data.str.string);
	inCmd->data.str.length = 0;
}

static VDM_Error getMngUnitId(char *outMngUnitId)
{
	VDM_Error ret = VDM_ERR_OK;
	SWM_PROT_TLV_Command_t tempCmd;
	IU8  *getNextEcuId = NULL;

	do
	{
		VDM_PL_memset(&tempCmd, '\0', sizeof(tempCmd));
		ret = getCompIdInfo(E_SWM_PROT_Tag_getEcuId, getNextEcuId,
			(char **)&tempCmd.comp_id.string, (IU32 *)&tempCmd.comp_id.length);
		if (ret != VDM_ERR_OK)
		{
			VDM_logError(("getCompIdInfo failed %d", ret));
			break;
		}

		// get management unit from current ECU HW ID
		ret = getEcuHwAttribute(tempCmd.comp_id.string,
			E_SWM_PROT_Tag_getManagementUnit, outMngUnitId);
		if (ret != VDM_ERR_OK)
		{
			VDM_logError(("getEcuHwAttribute failed %d", ret));
			break;
		}

		if (*outMngUnitId)
		{
			break;//found management unit
		}
		// release the previous id
		VDM_PL_freeAndNullify(getNextEcuId);
		// set the current id to get the next one
		getNextEcuId = (IU8 *)VDM_UTL_strndup((const char *)tempCmd.comp_id.string,
			tempCmd.comp_id.length);
		releaseCmd(&tempCmd);
	} while (getNextEcuId);

	releaseCmd(&tempCmd);
	VDM_PL_freeAndNullify(getNextEcuId);
	return ret;
}

static void setDataTypeInfo(SWM_PROT_TLV_Command_t *inCmd, SWM_PROT_TLV_Command_t *outCmd)
{
	switch (inCmd->data_type)
	{
	case E_SWM_PROT_DataType_str:
	{
		if (inCmd->data.str.length > 0)
		{
			outCmd->data.str.string = (const unsigned char *)VDM_UTL_strndup((const char *)inCmd->data.str.string, (IU32)inCmd->data.str.length);
			outCmd->data.str.length = inCmd->data.str.length;
		}
		break;
	}
	case E_SWM_PROT_DataType_bin:
	{
		if (inCmd->data.bin.size > 0)
		{
			outCmd->data.bin.data = VDM_PL_malloc((IU32)inCmd->data.bin.size);
			VDM_PL_memcpy(outCmd->data.bin.data, inCmd->data.bin.data, (IU32)inCmd->data.bin.size);
			outCmd->data.bin.size = inCmd->data.bin.size;
		}
		break;
	}
	case E_SWM_PROT_DataType_num:
	{
		outCmd->data.num.number = inCmd->data.num.number;
		break;
	}
	}
}

///////////////////////////////////////////////////////
//////////////// PL FUNCTIONS /////////////////////////
///////////////////////////////////////////////////////
VDM_Error SWMC_PL_ECU_init(void **inContext)
{
	VDM_logDebug(("+SWMC_PL_ECU_init"));
	return VDM_ERR_OK;
}

VDM_Error SWMC_PL_ECU_request(void *inContext, SWM_PROT_TLV_Command_t *inCmd)
{
	VDM_Error ret = VDM_ERR_OK;

	VDM_logDebug(("+SWMC_PL_ECU_request,  inCmd:0x0%x component installer type:%d",
		inCmd->tag, inCmd->ins_type));
	// memory release command, just release and return;
	if (E_SWM_PROT_Tag_releaseMemory == inCmd->tag)
	{
		releaseCmd(inCmd);
		goto end;
	}
	// check if sleep is needed and do it here, if fails we return error
	ret = sleepSimulate(inCmd->tag);
	if (ret != VDM_ERR_OK && ret != VDM_ERR_STORAGE_OPEN)
	{
		goto end;
	}

	// set the current tag request
	s_currentCmd.tag = inCmd->tag;
	s_currentCmd.data_type = inCmd->data_type;
	s_currentCmd.ins_phase = inCmd->ins_phase;
	s_currentCmd.ins_type = inCmd->ins_type;

	if (inCmd->tag == E_SWM_PROT_Tag_init)
	{
		goto end;
	}

	if (inCmd->tag == E_SWM_PROT_Tag_install ||
	    inCmd->tag == E_SWM_PROT_Tag_update ||
	    inCmd->tag == E_SWM_PROT_Tag_fuseUpdate ||
	    inCmd->tag == E_SWM_PROT_Tag_externalInstall) // meaning install requested
	{
		char planFile[MAX_PATH] = {'\0'};
		generatePlanFileName(inCmd->ins_phase, (const char *)inCmd->data.str.string, planFile);
		if (createUpdateSimContext(&s_updateSimContext, planFile) != VDM_ERR_OK)
		{
			s_updateSimContext = NULL;
		}
	}

	if (inCmd->tag == E_SWM_PROT_Tag_getCompId
	    || inCmd->tag == E_SWM_PROT_Tag_getEcuId)    // meaning we have working id
	{
		getCompIdInfo(inCmd->tag,
			(IU8 *)inCmd->comp_id.string,
			(char **)&s_currentCmd.comp_id.string,
			(IU32 *)&s_currentCmd.comp_id.length);
	}
	else
	{
		s_currentCmd.comp_id.length = inCmd->comp_id.length;
		VDM_PL_freeAndNullify(s_currentCmd.comp_id.string);
		s_currentCmd.comp_id.string =
		    (const unsigned char *)VDM_UTL_strndup((const char *)inCmd->comp_id.string,
			(IU32)inCmd->comp_id.length);
	}

	setDataTypeInfo(inCmd, &s_currentCmd);

end:
	VDM_logDebug(("-SWMC_PL_ECU_request return %d", ret));

	return ret;
}

VDM_Error SWMC_PL_ECU_response(void *inContext, SWM_PROT_TLV_Command_t *outCmd)
{
	char deltaPath [MAX_PATH + 1] = {'\0'};
	char ecuData [MAX_PATH + 1] = {'\0'};

	// verify that we fill the correct comp_id for the response
	if (outCmd->comp_id.string == NULL)
	{
		// set current working ID
		outCmd->comp_id.string =  (const unsigned char *)VDM_UTL_strndup(
			(const char *)s_currentCmd.comp_id.string,
			(IU32)s_currentCmd.comp_id.length);
		outCmd->comp_id.length =  s_currentCmd.comp_id.length;
	}

	outCmd->ins_phase = s_currentCmd.ins_phase;
	outCmd->ins_type = s_currentCmd.ins_type;

	switch (s_currentCmd.tag)
	{
	case E_SWM_PROT_Tag_getVersion:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag %d is handled", s_currentCmd.tag));
		getCompVersion((IU8 *)outCmd->comp_id.string,
			(char **)&outCmd->data.str.string,
			(IU32 *)&outCmd->data.str.length);
		outCmd->tag = s_currentCmd.tag;
		outCmd->data_type = E_SWM_PROT_DataType_str;
		break;
	}
	case E_SWM_PROT_Tag_install:
	{
		VDM_PL_strncpy(deltaPath, (const char *)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_install is handled on delta: %s", deltaPath));
		updateEcuPL(outCmd);

		if (outCmd->tag == E_SWM_PROT_Tag_progress)
		{
			goto end;    // we do not want to remove current command, only when install is finished
		}
		break;
	}
	case E_SWM_PROT_Tag_update:
	{
		VDM_PL_strncpy(deltaPath, (const char *)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_update is handled for DP %s", deltaPath));
		updateEcuPL(outCmd);

		if (outCmd->tag == E_SWM_PROT_Tag_progress)
		{
			goto end;    // we do not want to remove current command, only when update is finished
		}
		break;
	}
	case E_SWM_PROT_Tag_fuseUpdate:
	{
		VDM_PL_strncpy(deltaPath, (const char *)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_fuseUpdate is handled for DP %s", deltaPath));
		updateEcuPL(outCmd);

		if (outCmd->tag == E_SWM_PROT_Tag_progress)
		{
			goto end;    // we do not want to remove current command, only when update is finished
		}
		break;
	}
	case E_SWM_PROT_Tag_externalInstall:
	{
		VDM_PL_strncpy(deltaPath, (const char *)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_externalInstall is handled for DP %s", deltaPath));
		updateEcuPL(outCmd);

		if (outCmd->tag == E_SWM_PROT_Tag_progress)
		{
			goto end;    // we do not want to remove current command, only when update is finished
		}
		break;
	}
	case E_SWM_PROT_Tag_remove:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_remove is handled"));
		outCmd->tag = s_currentCmd.tag;
		outCmd->data.num.number = 0;     // remove is success
		outCmd->data_type = E_SWM_PROT_DataType_num;
		break;
	}
	case E_SWM_PROT_Tag_sourceDataPath:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag E_SWM_PROT_Tag_data is handled"));
		outCmd->tag = s_currentCmd.tag;
		outCmd->data.str.length = (unsigned int)VDM_PL_strlen((char *)ECU_SOURCE_ROM_PATH);
		outCmd->data.str.string = (const unsigned char *)VDM_UTL_strndup(
			ECU_SOURCE_ROM_PATH,
			(IU32)outCmd->data.str.length);
		outCmd->data_type = E_SWM_PROT_DataType_str;
		break;
	}
	case E_SWM_PROT_Tag_init:
	case E_SWM_PROT_Tag_term:
	case E_SWM_PROT_Tag_getEcuId:
	case E_SWM_PROT_Tag_getCompId:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag %d is handled", s_currentCmd.tag));
		outCmd->tag = s_currentCmd.tag;
		break;
	}
	case E_SWM_PROT_Tag_getNodeAddress:
	case E_SWM_PROT_Tag_getSerialNumber:
	case E_SWM_PROT_Tag_getPartNumber:
	case E_SWM_PROT_Tag_getManagementUnit:
	{
		outCmd->tag = s_currentCmd.tag;
		VDM_logDebug(("SWMC_PL_ECU_response tag %d is handled", s_currentCmd.tag));

		if (s_currentCmd.tag == E_SWM_PROT_Tag_getManagementUnit)
		{
			getMngUnitId(ecuData);
		}
		else
		{
			getEcuHwAttribute(s_currentCmd.comp_id.string, s_currentCmd.tag, ecuData);
		}

		setDupStringToCmdStringData(&outCmd->data_type, &outCmd->data.str, (char *)ecuData);
		break;
	}
	case E_SWM_PROT_Tag_getName:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag %d is handled", s_currentCmd.tag));
		getCompName((IU8 *)outCmd->comp_id.string,
			(char **)&outCmd->data.str.string,
			(IU32 *)&outCmd->data.str.length);
		outCmd->tag = s_currentCmd.tag;
		outCmd->data_type = E_SWM_PROT_DataType_str;
		break;
	}
	default:
	{
		VDM_logDebug(("SWMC_PL_ECU_response tag not handled"));
		break;
	}
	}

	releaseCmd(&s_currentCmd);

end:
	VDM_logDebug(("-SWMC_PL_ECU_response"));
	return VDM_ERR_OK;
}

void SWMC_PL_ECU_term(void *inContext)
{
	VDM_logDebug(("+SWMC_PL_ECU_term"));
	if (s_updateSimContext)
	{
		destoryUpdateSimContext(s_updateSimContext);
		s_updateSimContext = NULL;
	}
	return;
}
