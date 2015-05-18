/*
 *******************************************************************************
 *
 * vdm_client_pl_dlpkg.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_dlpkg.c
 *
 * \brief DLOTA Download object
 *******************************************************************************
 */

#define _FILE_OFFSET_BITS 64
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_pl_alloc.h>
#include <vdm_client_pl_assert.h>
#include <vdm_pl_memory.h>
#include <vdm_pl_string.h>
#include <vdm_client_pl_log_levels.h>
#include <vdm_client_pl_log.h>
#include <vdm_client_pl_dlpkg.h>
#include "vdm_client_pl_linux_file.h"
#include <vdm_pl_string_utils.h>

#ifdef VDM_QNX
#include <sys/statvfs.h>
#else
#include <sys/statfs.h>
#endif


//--------------------------
//	types and definitions
//--------------------------

#define VDM_PL_DP_DefaultFilename	"vdm_update"
static char *s_dpFullPath = NULL;
static int s_refCount = 0;
//
// =========================
// Local functions
// =========================
static char *VDM_PL_DLObj_allocNameFromId(const char *inId)
{
	char *filename = NULL;
	IU32 i;

	filename = VDM_PL_malloc(VDM_PL_strlen(inId)+1);
	if (!filename)
		return NULL;

	VDM_PL_strcpy(filename, inId);
	if (filename[0] == '.')
		filename[0] = '_';

	for (i = 0; filename[i]; i++)
	{
		if (filename[i] == '/')
			filename[i] = '_';
	}
	return filename;
}

static char *_dup(const char *in)
{
	char * out = (char *)VDM_PL_malloc(VDM_PL_strlen(in)+1);
	
	if (!out)
		return NULL;
	VDM_PL_strcpy(out, in);
	return out;
}

static VDM_Error VDM_PL_DLObj_allocateFullPathFile(const char* inFileName, char **outFileName)
{
	VDM_Error result = VDM_ERR_OK;
	
	if (s_dpFullPath && *s_dpFullPath)
		*outFileName = VDM_PL_strjoin("/", s_dpFullPath, inFileName, NULL);
	else
		*outFileName = _dup(inFileName);
	if (!*outFileName)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error, ("VDM_PL_DLObj_allocateFullPathFile: allocate memory for file name"));
		result = VDM_ERR_MEMORY;
		goto end;
	}
	VDM_Client_PL_log(E_VDM_LOGLEVEL_Debug, ("VDM_PL_DLObj_allocateFullPathFile: filename: %s", *outFileName));

end:
	return result;
}

//
// =========================
// Implementation
// =========================

//-----------------------------------------------------
// VDM_Client_PL_Dlpkg_setFullPath
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_setFullPath(const char *inPkgPath)
{
	VDM_Error result = VDM_ERR_OK;

	s_refCount++;
	VDM_Client_PL_log(E_VDM_LOGLEVEL_Info, ("VDM_Client_PL_Dlpkg_setFullPath, s_refCount:%d", s_refCount));
	VDM_PL_freeAndNullify(s_dpFullPath);
	if (inPkgPath)
	{
		s_dpFullPath = _dup(inPkgPath);
		if (!s_dpFullPath)
		{
			result = VDM_ERR_MEMORY;
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Info, ("VDM_Client_PL_Dlpkg_setFullPath could not allocate memory"));
			goto end;
		}
	}
end:
	return result;
}

///-----------------------------------------------------
// VDM_Client_PL_Dlpkg_clearFullPath
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_clearFullPath()
{
	s_refCount--;
	VDM_Client_PL_log(E_VDM_LOGLEVEL_Info, ("VDM_Client_PL_Dlpkg_clearFullPath, s_refCount:%d", s_refCount));

	if(!s_refCount)
		VDM_PL_freeAndNullify(s_dpFullPath);
	return VDM_ERR_OK;
}

///-----------------------------------------------------
// VDM_Client_PL_Dlpkg_create
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_create(UTF8CStr inURI, char **outDlpkgHandle)
{
	VDM_Error result = VDM_ERR_OK;

	VDM_Client_PL_assert(outDlpkgHandle);

	if (!inURI)
	{
		*outDlpkgHandle = NULL;
	}
	else
	{
		*outDlpkgHandle = VDM_PL_DLObj_allocNameFromId((char *)inURI);
		if (! *outDlpkgHandle)
		{
			result = VDM_ERR_MEMORY;
		}
	}

	return result;
}

///-----------------------------------------------------
// VDM_Client_PL_Dlpkg_writeChunk
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_writeChunk(
	const char *inDlpkgHandle,
	IU32		inOffset,
	IU8*		inData,
	IU32		inDataSize,
	IU32 		*outOffset)
{
	VDM_Error	result = VDM_ERR_OK;
	FILE*		file = NULL;
	const char *filename = inDlpkgHandle? inDlpkgHandle : VDM_PL_DP_DefaultFilename;
	char *filenameFullPath = NULL;

	VDM_Client_PL_log(E_VDM_LOGLEVEL_Info, ("Download Data: Writing %u bytes from offset %u", inDataSize, inOffset));

	result = VDM_PL_DLObj_allocateFullPathFile(filename, &filenameFullPath);
	if (result)
		goto end;

	if (!filenameFullPath)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}

	/* first open the file */
	VDM_Client_linux_fopen(filenameFullPath, ((inOffset == 0) ? "wb" : "r+b"), &file);
	if (!file)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error, 
			("Download Data: Failed to open update file %s", filenameFullPath));
		result = VDM_ERR_STORAGE_OPEN;
		goto end;
	}

	/* Next: try to move to correct position; fail if the file is too short */
	if (lseek(fileno(file), (off_t)inOffset, SEEK_SET) == (off_t)-1)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
			("Download Data: Failed to access update file at offset %u", inOffset));
		result = VDM_ERR_STORAGE_SEEK;
		goto end;
	}

	/* Next: write the buffer*/
	if (fwrite(inData, sizeof(IU8), inDataSize, file) != inDataSize)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
			("Download Data: Failed to write to update file"));
		result = VDM_ERR_STORAGE_WRITE;
	}

end:
	/* Close the file */
	if (file)
	{
		VDM_Client_linux_fclose(&file);
	}

	/*Finally: get the size of the file*/
	if (outOffset)
	{
		*outOffset = inOffset + inDataSize;
	}
	VDM_PL_freeAndNullify(filenameFullPath);

	return result;
}

//---------------------------------------------------------------
//VDM_Client_PL_Dlpkg_getPkgSize
//---------------------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_getPkgSize(const char *inURI, IU32 *outSize)
{
	struct stat statBuffer;
	VDM_Error	result = VDM_ERR_OK;
	char*		filename = NULL;
	char*		filenameFullPath = NULL;

	if (inURI)
		filename = VDM_PL_DLObj_allocNameFromId(inURI);
	else
		filename = _dup(VDM_PL_DP_DefaultFilename);

	if (!filename)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}

	result = VDM_PL_DLObj_allocateFullPathFile(filename, &filenameFullPath);
	if (result)
		goto end;

	if (!filenameFullPath)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}
    *outSize = 0;

	if (stat(filenameFullPath, &statBuffer) == 0)
	{
        if (statBuffer.st_size < 0)
        {
            VDM_Client_PL_log(E_VDM_LOGLEVEL_Error, ("getPkgSize returned an invalid or overflowed value"));
            result = VDM_ERR_STORAGE_READ;
        }
        else
		    *outSize = (IU32)statBuffer.st_size;
	}
	else if (errno != ENOENT)//if the file exists
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error, ("Download Data: Failed to get the file statistics"));
		result = VDM_ERR_STORAGE_READ;
	}
end:
	VDM_PL_freeAndNullify(filenameFullPath);
	VDM_PL_freeAndNullify(filename);
	return result;
}

///-----------------------------------------------------
// VDM_Client_PL_Dlpkg_remove
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_remove(const char *inDlpkgHandle)
{
	char* filenameFullPath = NULL;
	VDM_Error result = VDM_ERR_OK;
	const char *filename = inDlpkgHandle? inDlpkgHandle : VDM_PL_DP_DefaultFilename;

	result = VDM_PL_DLObj_allocateFullPathFile(filename, &filenameFullPath);
	if (result)
		goto end;

	result = !remove(filenameFullPath) ? VDM_ERR_OK : VDM_ERR_UNSPECIFIC;
end:
	VDM_PL_freeAndNullify(filenameFullPath);
	return result;
}

//-----------------------------------------------------
//VDM_Client_PL_Dlpkg_getMaxSize
//-----------------------------------------------------
VDM_Error VDM_Client_PL_Dlpkg_getMaxSize(IU32 *outDlpkgMaxSize)
{
#ifdef VDM_QNX
	#define stat_fs statvfs
#else
	#define stat_fs statfs
#endif
	struct stat_fs buf = {0};
	
	if (stat_fs(".", &buf) || buf.f_bsize <= 0)
		return VDM_ERR_UNSPECIFIC;
	if ((MAX_IU32 / (IU32)buf.f_bsize) < buf.f_bfree)
		*outDlpkgMaxSize = MAX_IU32;
	else
		*outDlpkgMaxSize = (IU32)buf.f_bsize * (IU32)buf.f_bfree;
	return VDM_ERR_OK;
}

