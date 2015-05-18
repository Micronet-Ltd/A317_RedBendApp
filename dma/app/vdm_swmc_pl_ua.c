/*
 *******************************************************************************
 *
 * vdm_swmc_pl_ua.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_swmc_pl_ua.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <string.h>
#ifdef VDM_QNX
#include <sys/statvfs.h>
#else
#include <sys/statfs.h>
#endif
#include <vdm_components.h>
#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_pl_stdlib.h>
#include <vdm_pl_string.h>
#include <vdm_client_pl_storage.h>
#include <swm_general_errors.h>
#include <vdm_swmc_pl_ua.h>
#include <vdm_utl_logger.h>
#include <vdm_utl_calloc.h>
#include <vdm_utl_utf8.h>

#define VDM_COMPONENT_ID	(E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC

#define NEEDED_SPACE 0x100000

//make sure there is enough space for vRM backup file
static IBOOL isFreeSpaceForUABackup(UTF8CStr inHandoffDir)
{
#ifdef VDM_QNX
	#define stat_fs statvfs
#else
	#define stat_fs statfs
#endif
	struct stat_fs statfsInfo = {0};
	IBOOL isFreeSpace = FALSE;
	
	if (stat_fs((const char*)inHandoffDir, &statfsInfo) == -1)
	{
		VDM_logDebug(("isFreeSpaceForUABackup: statfs on dir %s failed. error %s",
					inHandoffDir, strerror(errno)));
		goto end;
	}
	
	if (((IU32)statfsInfo.f_bsize * statfsInfo.f_blocks) >= NEEDED_SPACE)
		isFreeSpace = TRUE;
	
end:
	return isFreeSpace;
}


SWM_Error VDM_SWMC_PL_UA_handoff(char *inDpPath, char *inDpPathFile, UTF8CStr inHandoffDir)
{
	SWM_Error result = SWM_ERR_BAD_INPUT;
	IU32 dpPathLen = 0;
	void *handle = NULL;

	if (!inDpPath)
	{
		VDM_logDebug(("VDM_SWMC_PL_UA_handoff: inDpPath == NULL\n"));
		goto end;
	}

	VDM_logDebug(("VDM_SWMC_PL_UA_handoff: inDpPath = %s, inDpPathFile = %s, inHandoffDir = %s\n", \
			inDpPath, inDpPathFile, inHandoffDir));

	if (mkdir((const char*)inHandoffDir, 0755))
	{
		if (errno != EEXIST)
		{
			VDM_logDebug(("VDM_SWMC_PL_UA_handoff: cannot mkdir %s. error %s", \
					inHandoffDir, strerror(errno)));
			goto end;
		}
	}
	
	if (!isFreeSpaceForUABackup(inHandoffDir))
	{
		result = SWM_ERR_NO_FS_SPACE_AVAILABLE;
		goto end;
	}
	
	if (VDM_Client_PL_Storage_openByName(&handle, inDpPathFile, E_VDM_CLIENT_PL_Storage_Access_write) != VDM_ERR_OK)
	{
		VDM_logDebug(("VDM_SWMC_PL_UA_handoff: cannot open file %s. error %s", \
			inDpPathFile, strerror(errno)));
		result = SWM_ERR_ENV;
		goto end;
	}

	dpPathLen = VDM_PL_strlen(inDpPath);
	result = SWM_ERR_OK;

	if (VDM_Client_PL_Storage_write(handle, inDpPath, dpPathLen) != VDM_ERR_OK)
		result = SWM_ERR_ENV;

end:
	if (handle)
	{
		VDM_Client_PL_Storage_sync(handle);
		VDM_Client_PL_Storage_close(handle, TRUE);
	}
	return result;
}

SWM_Error VDM_SWMC_PL_UA_getResult(IU32 *outUpdateResult, char *inResultFile, char *inDpPathFile)
{
	SWM_Error result = SWM_ERR_BAD_INPUT;
	IU8 resultBuffer[10] = {0};
	IU32 resultBufferSize = sizeof(resultBuffer);
	void *handle = NULL;

	if (!outUpdateResult)
		goto end;

	if (VDM_Client_PL_Storage_openByName(&handle, inResultFile, E_VDM_CLIENT_PL_Storage_Access_read) != VDM_ERR_OK)
	{
		VDM_logDebug(("VDM_SWMC_PL_UA_getResult: VDM_Client_PL_Storage_openByName failed."
				"inResultFile = %s, result = 0x%x\n", inResultFile, result));
		result = SWM_ERR_ENV;
		goto end;
	}

	if (!VDM_Client_PL_Storage_fgets(handle, resultBuffer, resultBufferSize))
	{
		VDM_logDebug(("VDM_SWMC_PL_UA_getResult: VDM_Client_PL_Storage_fgets failed.\n"));
		result = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	VDM_logDebug(("VDM_SWMC_PL_UA_getResult: resultBuffer = %s.\n", resultBuffer));

	if (resultBuffer[0] == '0')
		*outUpdateResult = SWM_ERR_OK;
	else
		*outUpdateResult = SWM_ERR_UNSPECIFIC;

	result = SWM_ERR_OK;
end:
	if (handle)
	{
		VDM_Client_PL_Storage_close(handle, FALSE);

		/* Files are deleted only after they are closed */
		VDM_Client_PL_Storage_deleteByName(inResultFile);
		VDM_Client_PL_Storage_deleteByName(inDpPathFile);
	}

	return result;
}
