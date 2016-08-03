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

#ifndef VDM_QNX
#include <sys/errno.h>
#include <sys/vfs.h>
#else
#include <errno.h>
#endif // VDM_QNX

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <limits.h>
#include <libgen.h>

#define VDM_COMPONENT_ID	(E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC

#define ECU_COMP_ID (const char *)"FUSE"
#define ECU_SOURCE_ROM_PATH ECU_COMP_ID"_SOURCE"

#define ECU_COMP_FILE_NAME "ecu_components.txt"
#define ECU_COMP_FILE_PLAN_SUFFIX "plan"
#define ECU_COMP_FILE_MAX_LINE 256
#define MAX_PATH 1024

#ifdef VDM_RUN_ON_ANDROID
#define SOCK_PATH "/dev/socket"
#else
#define SOCK_PATH "/tmp/redbend"
#endif

#define SOCK_NAME "TRANS_INS"
#define SOCK_FULL_NAME SOCK_PATH"/"SOCK_NAME

#define CHECK_RET_GOTO_END_PL(x) do{ \
	ret = (x); \
	if (ret != VDM_ERR_OK) \
	{ \
		VDM_logError(("Error %d [%s]\n", ret, strerror(errno))); \
		goto end; \
	} \
} while (0)

#define INST_HEADER "FUSE_INST"
#define TRANS_HEADER "FUSE_TRANS"

/**
 * The Installer <--> Transmitter protocol
 * FUSE_INST:UPDATE_PATH:path (char*)
 * FUSE_TRANS:UPDATE_PATH:len (int)
 *
 * FUSE_INST:REFLASH_PATH:path (char*)
 * FUSE_TRANS:REFLASH_PATH:len (int)
 *
 * FUSE_TRANS:PROGRESS:percentage (0-100)
 *
 * FUSE_TRANS:COMPLETE:res (int, 0 for OK)
*/

static char *updateFlavorCmd[3] = { "REFLASH_PATH", "", "UPDATE_PATH" };

// for the moment to get response
SWM_PROT_TLV_Command_t s_currentCmd;
// create internal command enum
typedef enum
{
	E_FILE_Command_error,
	E_FILE_Command_sleep,
	E_FILE_Command_progress,
	E_FILE_Command_updateResult
} E_FILE_Command_t;

///////////////////////////////////////////////////////////////////
//////////////// STATIC FUNCTIONS /////////////////////////////////
///////////////////////////////////////////////////////////////////

static VDM_Error transmitterConnect(UTF8Str socketPath, int *sockfd)
{
	struct sockaddr_un addr;
	IU32 len;

	if ((*sockfd = socket(AF_UNIX, SOCK_STREAM, 0)) < 0)
	{
		VDM_logError(("transmitterConnect: Failed to open socket, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	VDM_PL_memset(&addr, 0, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;
	VDM_PL_strncpy(addr.sun_path, (char*)socketPath, sizeof(addr.sun_path)-1);

	len = VDM_PL_strlen(addr.sun_path) + sizeof(addr.sun_family);
	if (connect(*sockfd, (struct sockaddr *)&addr, (socklen_t)len) < 0) {
		VDM_logError(("transmitterConnect: Failed to connect to socket %s, %s", socketPath, strerror(errno)));
		close(*sockfd);
		return VDM_ERR_UNSPECIFIC;
	}

	return VDM_ERR_OK;
}

static void transmitterDisconnect(int sockfd)
{
	if (sockfd)
		close(sockfd);
}

static VDM_Error transmitterWrite(int sockfd, void *buf, IU32 sz)
{
	
	VDM_logDebug(("+transmitterWrite"));

	if (write(sockfd, (void*)&sz, sizeof(IU32)) != sizeof(IU32))
	{
		VDM_logError(("transmitterWrite: failed to send message length, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	if (write(sockfd, buf, sz) != sz)
	{
		VDM_logError(("transmitterWrite: failed to send message, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	VDM_logDebug(("-transmitterWrite"));
	return VDM_ERR_OK;
}

static VDM_Error transmitterRead(int sockfd, void *buf, size_t sz)
{
	IS32 len;
	IU32 msgLen = 0;

	VDM_logDebug(("+transmitterRead"));

	if (read(sockfd, &msgLen, sizeof(IU32)) != sizeof(IU32))
	{
		VDM_logError(("transmitterRead: failed to get message length, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	if (msgLen > sz)
	{
		VDM_logError(("transmitterRead: %d > %d", msgLen, sz));
		return VDM_ERR_BUFFER_OVERFLOW;
	}

	if ((len = read(sockfd, buf, msgLen)) != msgLen)
	{
		VDM_logError(("transmitterRead: failed to read message, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	VDM_logDebug(("-transmitterRead"));

	return VDM_ERR_OK;
}

static VDM_Error transmitterGetEcuVersion (int sockfd, UTF8Str outBuffer, IU32 inBufferSize)
{
	char msg[256] = {0,}, transHeader[256], transCmd[256], deviceVer[256] = "0xFFFFFFFF";
	int transRes, msgLen = 0;

	VDM_logDebug(("+transmitterGetEcuVersion"));

	msgLen = sprintf(msg, "%s:%s", INST_HEADER, "GET_VERSION");
	if (transmitterWrite(sockfd, msg, (size_t)msgLen) != VDM_ERR_OK)
	{
		VDM_logError(("transmitterGetEcuVersion: Failed to send GET_VERSION command, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	// Wait for version
	if (transmitterRead(sockfd, msg, sizeof(msg)-1) != VDM_ERR_OK)
	{
		VDM_logError(("transmitterGetEcuVersion: failed to get version from transmitter, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	//Check returned message correctness
	sscanf(msg, "%[^:]:%[^:]:%d:%s", transHeader, transCmd, &transRes, deviceVer);
	if (VDM_PL_strncmp(transHeader, TRANS_HEADER, sizeof(TRANS_HEADER)) ||
		VDM_PL_strcmp(transCmd, "GET_VERSION") || transRes != 0)
	{
		VDM_logError(("transmitterGetEcuVersion: Got wrong message from transmitter %s, %s", msg, strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	VDM_PL_strncpy((char*)outBuffer, deviceVer, inBufferSize);
	VDM_logDebug(("-transmitterGetEcuVersion returned version is %s", outBuffer));
	return VDM_ERR_OK;
}

static VDM_Error sendStartUpdateRequest(int sockfd, E_SWM_PROT_Tag_t flavor,
		const UTF8Str dpPathFile, unsigned int inDpPathFileLength)
{
	char msg[ECU_COMP_FILE_MAX_LINE] = {0,}, \
		 transHeader[ECU_COMP_FILE_MAX_LINE], \
		 transCmd[ECU_COMP_FILE_MAX_LINE], \
		 fullDeltaFilePath[ECU_COMP_FILE_MAX_LINE];
	IS32 msgLen;
	IU32 transData = 0;
	int operation = 0;
	VDM_Error ret = VDM_ERR_OK;

	VDM_PL_strlcpy(fullDeltaFilePath, (const char *)dpPathFile, (IU32)inDpPathFileLength + 1); 
	//Send start update message to transmitter
	operation = (flavor == E_SWM_PROT_Tag_install) ? 0 : 2;  // check if install\REFLASH or update/UPDATE
	msgLen = sprintf(msg, "%s:%s:%s", INST_HEADER, updateFlavorCmd[operation], fullDeltaFilePath);

	VDM_logDebug(("+sendStartUpdateRequest msg is: %s", msg));

	if (transmitterWrite(sockfd, msg, (size_t)msgLen) != VDM_ERR_OK)
	{
		VDM_logError(("sendStartUpdateRequest: Failed to start the update %d, %s", flavor, strerror(errno)));
		ret = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	// Wait for ACK from transmitter
	if (transmitterRead(sockfd, msg, sizeof(msg)-1) != VDM_ERR_OK)
	{
		VDM_logError(("sendStartUpdateRequest: failed to get ack from transmitter, %s", strerror(errno)));
		ret = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	//Check returned message correctness
	sscanf(msg, "%[^:]:%[^:]:%lu", transHeader, transCmd, &transData);
	if (VDM_PL_strncmp(transHeader, TRANS_HEADER, sizeof(TRANS_HEADER)) ||
		VDM_PL_strncmp(transCmd, updateFlavorCmd[operation], VDM_PL_strlen(updateFlavorCmd[operation])) ||
		VDM_PL_strlen(fullDeltaFilePath) != transData)
	{
		VDM_logError(("sendStartUpdateRequest: Got wrong message from transmitter %s, %s", msg, strerror(errno)));
		ret = VDM_ERR_UNSPECIFIC;
	}

end:

	VDM_logDebug(("-sendStartUpdateRequest result is: %d", ret));

	return ret;
}

static VDM_Error getUpdateResponse(int sockfd, char *cmd, IU32 *data)
{
	char msg[256] = {0,};
	char transHeader[256]= {0,};

	if (transmitterRead(sockfd, msg, sizeof(msg)-1) != VDM_ERR_OK )
	{
		VDM_logError(("getUpdateResponse: failed to read progress from transmitter, %s", strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	sscanf(msg, "%[^:]:%[^:]:%lu", transHeader, cmd, data);
	if (VDM_PL_strncmp(transHeader, TRANS_HEADER, sizeof(TRANS_HEADER)))
	{
		VDM_logError(("getUpdateResponse: Got wrong message from transmitter %s, %s", msg, strerror(errno)));
		return VDM_ERR_UNSPECIFIC;
	}

	return VDM_ERR_OK;
}

static E_FILE_Command_t parseResponse(IU8 *request, IU8 *value, int *outValue)
{
	// convert value to integer
	IBOOL convertStr = FALSE;
	*outValue = (int)VDM_PL_atoIU32((const char* )value, (IU8)10, &convertStr);
	if (convertStr == FALSE)
	{
		goto end; 
	}
	if (VDM_PL_strcmp((char *)(request), "E_FILE_Command_progress") == 0)
	{
			return E_FILE_Command_progress;
	}
	else if (VDM_PL_strcmp((char *)(request), "E_FILE_Command_updateResult") == 0)
	{
		return E_FILE_Command_updateResult;
	}
	else if (VDM_PL_strcmp((char *)(request), "E_FILE_Command_sleep") == 0)
	{
		return E_FILE_Command_sleep;
	}
	else if (VDM_PL_strcmp((char *)(request), "E_FILE_Command_error") == 0)
	{
		return E_FILE_Command_error;
	}

end:
	// problem occured return error
	*outValue = 1500;
	return E_FILE_Command_error;
}

static  E_SWM_PROT_Tag_t getFileResponse(char *inDcFilePlan, int *retResponseValue)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error result = VDM_ERR_OK;
	int returnNum = 0;
	static void *handle = NULL; // we want to save the file open for next command request
	E_SWM_PROT_Tag_t ret = E_SWM_PROT_Tag_updateResult;
	*retResponseValue =  -1;

	if (handle == NULL)
	{
		result = VDM_Client_PL_Storage_openByName(&handle, (char *)inDcFilePlan, E_VDM_CLIENT_PL_Storage_Access_read);
		if (!handle || result != VDM_ERR_OK)
		{
			VDM_logError(("Cannot open file '%s' for reading", ECU_COMP_FILE_NAME));
			goto end;
		}
	}

	do
	{
		if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
		{
			VDM_logError(("finished reading with out Version from '%s'", ECU_COMP_FILE_NAME));
			goto end;
		}

		// set NULL in end of line instead of \n
		returnNum = (int)VDM_PL_strlen((const char *)buf);
		buf[returnNum - 1] = '\0';

		result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, '=', &key, &value); 
		if (value != NULL && key != NULL)// the same ID, need to get next one
		{
			switch (parseResponse(key, value, &returnNum))
			{
				case E_FILE_Command_error:
				{
					VDM_logDebug(("File Response returned error during installation of %d ", returnNum));
					// error occured we will send update fail
					ret = E_SWM_PROT_Tag_updateResult;
					*retResponseValue =  -1;
					goto end;
				}
				case E_FILE_Command_sleep:
				{
					// we will sleep and continue to next line
					VDM_logDebug(("File Response returned sleep request for %d ", returnNum));
					sleep((unsigned int)returnNum);
					continue; // to next line read
				}
				case E_FILE_Command_progress:
				{
					VDM_logDebug(("Response returned progress of %d ", returnNum));
					ret = E_SWM_PROT_Tag_progress;
					*retResponseValue =  returnNum;
					return ret; // we want to save the handle open till end of command lines
				}
				case E_FILE_Command_updateResult:
				{
					VDM_logDebug(("File Response returned update reulst of %d ", returnNum));
					ret = E_SWM_PROT_Tag_updateResult;
					*retResponseValue =  returnNum;
					goto end; // update finished
				}
				default:
				{
					VDM_logDebug(("File Response returned unknown command we send update result fail!!!"));
					ret = E_SWM_PROT_Tag_updateResult;
					*retResponseValue =  -1;
					goto end;

				}
			}
		}
		else
			goto end;// error occured

		// initialize buffer before next read 
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);

	}while(result == VDM_ERR_OK);

end:

	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);
	handle = NULL;

	return ret;

}

static void updateEcuPL(char *inDeltaPath, SWM_PROT_TLV_Command_t *outCmd)
{
	int fileResponseRetValue = 0;
	char *pCompIdName = NULL;
	char planFile[MAX_PATH] = {'\0'};
	char seperater = '.';

	outCmd->data_type = E_SWM_PROT_DataType_num;
	// get component name
	pCompIdName = VDM_PL_strchr((const char *)inDeltaPath, seperater);
	if (pCompIdName == NULL) // something is wrong, we fail
	{
		VDM_logDebug(("could not get char %c from file %s, we fail the update", seperater, inDeltaPath));
		outCmd->data.num.number = -1;
		outCmd->tag = E_SWM_PROT_Tag_updateResult;
		return;
	}

	*pCompIdName = '\0'; // set NULL after component name
	VDM_PL_snprintf(planFile, MAX_PATH, "%s.%s", inDeltaPath, ECU_COMP_FILE_PLAN_SUFFIX); 

	switch (getFileResponse(planFile, &fileResponseRetValue))
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
		{
			break;
		}
	}
}

/*
static VDM_Error getCompVersion(IU8 *inCompId, char **outVersion, IU32 *outVersionLength)
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

	do
	{
		if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
		{
			VDM_logError(("finished reading with out Version from '%s'", ECU_COMP_FILE_NAME));
			goto end;
		}

		result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, '=', &key, &value); 
		if (value != NULL && key != NULL)
		{
			if (VDM_PL_strcmp((const char*)key, (const char *)inCompId) == 0)
			{
				*outVersionLength = VDM_PL_strlen((const char*) value);
				*outVersion = VDM_UTL_strndup((const char *)value, *outVersionLength);
				break; // found our component ID
			}
		}
		// initialize buffer before next read 
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);

	}while(result == VDM_ERR_OK);

end:

	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);

	return ret;
}
*/
static VDM_Error getCompIdInfo(IU8 *inCompId, char **outCompId, IU32 *outCompIdLength)
{
	IU8 buf[ECU_COMP_FILE_MAX_LINE] = {'\0'};
	IU8 *key = NULL;
	IU8 *value = NULL;
	VDM_Error ret = VDM_ERR_OK;
	VDM_Error result = VDM_ERR_OK;
	void *handle = NULL;
	IBOOL currCompIdUse = FALSE;

	result = VDM_Client_PL_Storage_openByName(&handle, (char *)ECU_COMP_FILE_NAME, E_VDM_CLIENT_PL_Storage_Access_read);
	if (!handle || result != VDM_ERR_OK)
	{
		VDM_logError(("Cannot open file '%s' for reading", ECU_COMP_FILE_NAME));
		ret = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	do
	{
		if (NULL == VDM_Client_PL_Storage_fgets(handle, (void *)buf, (IU32)ECU_COMP_FILE_MAX_LINE))
		{
			VDM_logDebug(("finished reading lines from '%s'", ECU_COMP_FILE_NAME));
			goto end;
		}

		result = VDM_UTL_CfgParser_parsePair((UTF8Str)buf, '=', &key, &value); 
		if (key != NULL)// the same ID, need to get next one
		{
			if (inCompId == NULL)
			{
				// if no prev we use current component ID
				currCompIdUse = TRUE;
			}

			if (currCompIdUse == TRUE)
			{
				*outCompIdLength = VDM_PL_strlen((const char*) key);
				*outCompId = VDM_UTL_strndup((const char *)key, *outCompIdLength);
				break; // found our component ID
			}

			if (VDM_PL_strcmp((const char*)key, (const char *)inCompId) == 0) // need to find prev and then take next compId
			{
				// need to allocate prev ECU ID and then use the next one.
				currCompIdUse = TRUE;
			}
		}
		// initialize buffer before next read 
		VDM_PL_memset(buf, '\0', ECU_COMP_FILE_MAX_LINE);

	}while(result == VDM_ERR_OK);

end:

	// no need to commit as we only read from the file
	VDM_Client_PL_Storage_close(handle, FALSE);

	if (result != VDM_ERR_OK)
		ret = VDM_ERR_UNSPECIFIC;

	return ret;
}

static void releaseCmd(SWM_PROT_TLV_Command_t * inCmd)
{
	VDM_PL_freeAndNullify(inCmd->comp_id.string);
	inCmd->comp_id.length = 0;
	VDM_PL_freeAndNullify(inCmd->data.str.string);
	inCmd->data.str.length = 0;
	VDM_PL_freeAndNullify(inCmd->data.bin.data);
	inCmd->data.bin.data = 0;
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
				outCmd->data.str.length =inCmd->data.str.length; 
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
	int *fd = NULL;
	fd = VDM_PL_malloc(sizeof(int));
	VDM_logDebug(("+SWMC_PL_ECU_init"));
	if (!fd)
		return VDM_ERR_ENV;
	*inContext = (void *)fd;
	VDM_logDebug(("-SWMC_PL_ECU_init"));
	return VDM_ERR_OK;
}

VDM_Error SWMC_PL_ECU_request(void *inContext, SWM_PROT_TLV_Command_t *inCmd)
{
	VDM_Error ret = VDM_ERR_OK;
	int *sockfd = inContext;
	VDM_logDebug(("+SWMC_PL_ECU_write"));
	// memory release command, just release and return;
	if (E_SWM_PROT_Tag_releaseMemory == inCmd->tag)
	{
		releaseCmd(inCmd);
		goto end; 
	}
	// set the current tag request
	s_currentCmd.tag = inCmd->tag;
	s_currentCmd.data_type= inCmd->data_type;

	if (inCmd->tag == E_SWM_PROT_Tag_getCompId)// meaning we have working id[
	{
		// get the ID from the file
		getCompIdInfo((IU8*)inCmd->comp_id.string, 
			(char **)&s_currentCmd.comp_id.string,
			(IU32*)&s_currentCmd.comp_id.length);

		goto end; // no need to open communication for get component ID.
	}
	else
	{
		s_currentCmd.comp_id.length = inCmd->comp_id.length;
		s_currentCmd.comp_id.string =  
			(const unsigned char *)VDM_UTL_strndup((const char *)inCmd->comp_id.string, 
												(IU32)inCmd->comp_id.length);
	}

	setDataTypeInfo(inCmd, &s_currentCmd); 

	// connect to board
	CHECK_RET_GOTO_END_PL(transmitterConnect((UTF8Str)SOCK_FULL_NAME, sockfd));

	if (s_currentCmd.tag == E_SWM_PROT_Tag_install || 
			s_currentCmd.tag == E_SWM_PROT_Tag_update ||
			s_currentCmd.tag == E_SWM_PROT_Tag_fuseUpdate ||
				s_currentCmd.tag == E_SWM_PROT_Tag_externalInstall)
	{
		VDM_logDebug(("calling to sendStartUpdateRequest"));
		// we want to send the start update request before reeding the results in a loop
		CHECK_RET_GOTO_END_PL(sendStartUpdateRequest(*sockfd, 
										s_currentCmd.tag, 
										(const UTF8Str)s_currentCmd.data.str.string, 
										s_currentCmd.data.str.length));
	}


end:
	VDM_logDebug(("-SWMC_PL_ECU_write"));

	return ret;
}

static VDM_Error transmitterUpdateEcu(int inSockfd, SWM_PROT_TLV_Command_t *outCmd)
{
	VDM_Error ret = VDM_ERR_OK;
	IU32 progress;
	char cmd[ECU_COMP_FILE_MAX_LINE] = {0};

	VDM_logDebug(("+transmitterUpdateEcu socket is: %d", inSockfd));

	CHECK_RET_GOTO_END_PL(getUpdateResponse(inSockfd, cmd, &progress));

	if (!VDM_PL_strcmp(cmd, "PROGRESS"))
	{
		VDM_logDebug(("Update Response func returned %d, got %s with progress %lu", ret, cmd, progress));
		outCmd->data.num.number = (int)progress;
		outCmd->tag = E_SWM_PROT_Tag_progress;
		outCmd->data_type = E_SWM_PROT_DataType_num;
	}
	else if (!VDM_PL_strcmp(cmd, "COMPLETE"))
	{
		outCmd->data.num.number = 0; // at the moment alwayes success
		VDM_logDebug(("Update Response func returned %d, got %s with result is %d", ret, cmd, outCmd->data.num.number));
		outCmd->tag = E_SWM_PROT_Tag_updateResult;
		outCmd->data_type = E_SWM_PROT_DataType_num;
	}
	else
		VDM_logDebug(("read did not get COMPLETED or PROGRESS but , %s", cmd));

end:

	VDM_logDebug(("-transmitterUpdateEcu, func returns %d", ret));
	return ret;
}

VDM_Error SWMC_PL_ECU_response(void *inContext, SWM_PROT_TLV_Command_t *outCmd)
{
	char deltaPath [MAX_PATH] = {'\0'};
	char outBuffer [MAX_PATH] = {'\0'};
	VDM_Error ret = VDM_ERR_OK;
	int *sockfd = (int *)inContext;
	outCmd->type = E_SWM_PROT_Type_response;

	VDM_logDebug(("+SWMC_PL_ECU_read"));

	// verify that we fill the correct comp_id for the response 
	if (outCmd->comp_id.string == NULL) 
	{
		// set current working ID
		outCmd->comp_id.string =  (const unsigned char *)VDM_UTL_strndup(
								(const char *)s_currentCmd.comp_id.string, 
								(IU32)s_currentCmd.comp_id.length);
		outCmd->comp_id.length =  s_currentCmd.comp_id.length;
	}

	switch (s_currentCmd.tag)
	{
		case E_SWM_PROT_Tag_getVersion:
		{
			transmitterGetEcuVersion (*sockfd, (UTF8Str)outBuffer, (IU32)MAX_PATH);
/*			getCompVersion((IU8*)outCmd->comp_id.string, 
					(char **)&outCmd->data.str.string, 
					(IU32*)&outCmd->data.str.length);   */
			outCmd->tag = s_currentCmd.tag;
			outCmd->data_type = E_SWM_PROT_DataType_str;
			outCmd->data.str.length = VDM_PL_strlen((const char *)outBuffer);
			outCmd->data.str.string = (unsigned char *) VDM_UTL_strndup((const char *)outBuffer, (IU32)outCmd->data.str.length); 
			break;
		}
		case E_SWM_PROT_Tag_getCompId:
		{
			outCmd->tag = s_currentCmd.tag;
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_getCompId is handled"));
			break;
		}
		case E_SWM_PROT_Tag_install:
		{
			VDM_PL_strncpy(deltaPath, (const char*)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_install is handled on delta: %s sockfd is: %d", deltaPath, *sockfd));
			updateEcuPL(deltaPath, outCmd);

			CHECK_RET_GOTO_END_PL(transmitterUpdateEcu(*sockfd, outCmd));

			if (outCmd->tag == E_SWM_PROT_Tag_progress)
				goto end;// we do not want to remove current command, only when update is finished

			break;
		}
		case E_SWM_PROT_Tag_update:
		{
			VDM_PL_strncpy(deltaPath, (const char*)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_update is handled for DP %s", deltaPath));
			//updateEcuPL(deltaPath, outCmd);

			CHECK_RET_GOTO_END_PL(transmitterUpdateEcu(*sockfd, outCmd));

			if (outCmd->tag == E_SWM_PROT_Tag_progress)
				goto end;// we do not want to remove current command, only when update is finished
		
			break;
		}
	case E_SWM_PROT_Tag_fuseUpdate:
		{
			VDM_PL_strncpy(deltaPath, (const char*)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_fuseUpdate is handled on delta: %s sockfd is: %d", deltaPath, *sockfd));
			//updateEcuPL(deltaPath, outCmd);

			CHECK_RET_GOTO_END_PL(transmitterUpdateEcu(*sockfd, outCmd));

			if (outCmd->tag == E_SWM_PROT_Tag_progress)
				goto end;// we do not want to remove current command, only when update is finished

			break;
		}
		case E_SWM_PROT_Tag_externalInstall:
		{
			VDM_PL_strncpy(deltaPath, (const char*)s_currentCmd.data.str.string, s_currentCmd.data.str.length );
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_externalInstall is handled for DP %s", deltaPath));
			//updateEcuPL(deltaPath, outCmd);

			CHECK_RET_GOTO_END_PL(transmitterUpdateEcu(*sockfd, outCmd));

			if (outCmd->tag == E_SWM_PROT_Tag_progress)
				goto end;// we do not want to remove current command, only when update is finished
		
			break;
		}
		case E_SWM_PROT_Tag_remove:
		{
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_remove is handled"));
			outCmd->tag = s_currentCmd.tag;
			outCmd->data.num.number = 0; // remove is success
			outCmd->data_type = E_SWM_PROT_DataType_num;
			break;
		}
		case E_SWM_PROT_Tag_sourceDataPath:
		{
			VDM_logDebug(("SWMC_PL_ECU_read tag E_SWM_PROT_Tag_data is handled"));
			outCmd->tag = s_currentCmd.tag;
			outCmd->data.str.length = VDM_PL_strlen((char *)ECU_SOURCE_ROM_PATH);
			outCmd->data.str.string = (const unsigned char *)VDM_UTL_strndup(
							ECU_SOURCE_ROM_PATH, 
							(IU32)outCmd->data.str.length);
			outCmd->data_type = E_SWM_PROT_DataType_str;
			break;
		}
		default:
		{
			VDM_logDebug(("SWMC_PL_ECU_read tag not handled"));
			break;
		}

	}

	releaseCmd(&s_currentCmd);

end:
	VDM_logDebug(("-SWMC_PL_ECU_read returns %d", ret));
	return ret;
}

void SWMC_PL_ECU_term(void *inContext)
{
	int *sockfd = (int *)inContext;
	VDM_logDebug(("+SWMC_PL_ECU_term"));
	transmitterDisconnect(*sockfd);
	if (inContext)
		VDM_PL_free(inContext);

	return;
}
