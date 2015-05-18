/*
 *******************************************************************************
 *
 * vdm_client_pl_storage.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_storage.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#define _FILE_OFFSET_BITS 64
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>

#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_pl_alloc.h>
#include <vdm_pl_memory.h>
#include <vdm_pl_string.h>
#include <vdm_pl_string_utils.h>
#include <vdm_client_pl_assert.h>
#include <vdm_client_pl_log.h>
#include <vdm_client_pl_log_levels.h>
#include <vdm_client_pl_storage.h>
#include "vdm_client_pl_linux_file.h"

#define VDM_Client_PL_Filename_tree		"tree.xml"
#define VDM_Client_PL_TmpFileName_tree		"tree_new.xml"

#define VDM_Client_PL_Filename_registry		"reg.conf"
#define VDM_Client_PL_TmpFilename_registry	"reg_new.conf"

// MERGE note: removed unused config.txt with vSM-specific config
#define VDM_Client_PL_Filename_config		"vsm_settings.txt"

#define VDM_Client_PL_Filename_dlresume	"dlresume.dat"

#define VDM_Client_PL_tempSuffix        	"_temp"
#define VDM_Client_PL_tempSuffix_len    	5

/* globals */
const char *g_VDMTreeFile = NULL;
const char *g_VDMConfigFile = NULL;
const char *g_VDMRegistryFile = NULL;
const char *g_VDMTmpRegistryFile = NULL;
IU32 g_minFileSize = 0;

typedef struct
{
	char* filename;
	char* tmpFilename;
	FILE*		file;
	mode_t		mode;
	E_VDM_CLIENT_PL_Storage_Access_t storageAccess;
} VDM_Client_PL_StorageItem_t;

static char *getItemFilename(E_VDM_CLIENT_PL_StorageItem_t inType)
{
	const char *filename = NULL;

	switch(inType)
	{
	case E_VDM_CLIENT_PL_StorageItem_DMTree:
		filename = (g_VDMTreeFile && g_VDMTreeFile[0]) ? g_VDMTreeFile : VDM_Client_PL_Filename_tree;
		break;
	case E_VDM_CLIENT_PL_StorageItem_DLResume:
		filename = VDM_Client_PL_Filename_dlresume;
		break;
	case E_VDM_CLIENT_PL_StorageItem_Config:
		filename = (g_VDMConfigFile && g_VDMConfigFile[0]) ? g_VDMConfigFile : VDM_Client_PL_Filename_config;
		break;
	case E_VDM_CLIENT_PL_StorageItem_Registry:
		filename = (g_VDMRegistryFile && g_VDMRegistryFile[0]) ? g_VDMRegistryFile : VDM_Client_PL_Filename_registry;
		break;
	default:
		break;
	}
	return (char *)filename; /* remove const */
}

/*
 * Used by vSM.
 */
UTF8CStr VDM_Client_PL_Storage_getPathDup(E_VDM_CLIENT_PL_StorageItem_t inType)
{
	char *dup = NULL;
	const char* filename = getItemFilename(inType);
	VDM_Client_PL_assert(filename);

	if (filename)
	{
		IU32 len = VDM_PL_strlen(filename);
		dup = VDM_PL_malloc(len+1);
		if (dup)
		{
			VDM_PL_memcpy(dup, filename, len);
			dup[len] = '\0';
		}
	}
	return (UTF8CStr)dup;
}

static VDM_Error getItemTmpFilename(E_VDM_CLIENT_PL_StorageItem_t inType, char **outTmpFileName)
{
	VDM_Error result = VDM_ERR_OK;

	*outTmpFileName = NULL;

	switch(inType)
	{
	case E_VDM_CLIENT_PL_StorageItem_DMTree:
		*outTmpFileName = strdup(VDM_Client_PL_TmpFileName_tree);
		if (!(*outTmpFileName))
			result = VDM_ERR_MEMORY;
		break;
	case E_VDM_CLIENT_PL_StorageItem_Registry:
		*outTmpFileName = strdup(g_VDMTmpRegistryFile ? g_VDMTmpRegistryFile : VDM_Client_PL_TmpFilename_registry);
		if (!(*outTmpFileName))
			result = VDM_ERR_MEMORY;
		break;
	default:
		break;
	}
	return result;
}

static char *getTmpFilenameDup(const char *inFileName)
{
	char* tmpFilename = NULL;
	IU32 len = VDM_PL_strlen(inFileName)+ VDM_Client_PL_tempSuffix_len + 1;
	tmpFilename = VDM_PL_malloc(len);

	if (tmpFilename)
		VDM_PL_snprintf(tmpFilename, len, "%s%s", (char*)inFileName,  VDM_Client_PL_tempSuffix);

	return tmpFilename;
}

static IBOOL fileExists(const char *inFilename)
{
	IBOOL isExist = FALSE;
	FILE* f = fopen(inFilename, "rb");
	if (f)
	{
		isExist = TRUE;
		fclose(f);
	}
	return isExist;
}

VDM_Error VDM_Client_PL_Storage_loadFile(char *inFileName, void *outBuffer,
	IU32 inBufSize, IU32 *outReadCount, IU32 *outFileSize)
{
	VDM_Error result = VDM_ERR_OK;
	VDM_Client_PL_StorageItem_t* handle = NULL;
	IS32 ftellResult;

	if (!inFileName || !*inFileName || !outReadCount || !outFileSize)
	{
		result = VDM_ERR_BAD_INPUT;
		goto end;
	}

	*outReadCount = 0;
	*outFileSize = 0;

	result = VDM_Client_PL_Storage_openByName((void**)&handle, inFileName, E_VDM_CLIENT_PL_Storage_Access_read);
	if (!handle || result != VDM_ERR_OK)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
			("Cannot open file '%s' for reading", inFileName));
		result = VDM_ERR_STORAGE_OPEN;
		goto end;
	}

	/* move pointer to end of file */
	result = VDM_Client_PL_Storage_fileSeek(handle, 0, E_VDM_CLIENT_PL_FSeek_END);
	if (result != VDM_ERR_OK)
		goto end;

	/* get file size */
	ftellResult = ftell(handle->file);
	if (ftellResult < 0)
		goto end;
	*outFileSize = (IU32)ftellResult;

	/* move pointer to beginning of file so fread will read from the beginning */
	result = VDM_Client_PL_Storage_fileSeek(handle, 0, E_VDM_CLIENT_PL_FSeek_START);
	if (result != VDM_ERR_OK)
		goto end;

	/* we check outBuffer here and not at the beginning of the function,
	 * because the function may be called with null outBuffer (for example,
	 * for inquiring the file size) */
	if (outBuffer)
	{
		result = VDM_Client_PL_Storage_read(handle, outBuffer, inBufSize, outReadCount);

		/* check if fread succeeded */
		if (result != VDM_ERR_OK)
			goto end;
	}

	/* check if supplied buffer is too small */
	if (*outFileSize > inBufSize*sizeof(char))
		result = VDM_ERR_BUFFER_OVERFLOW;

end:
   	VDM_Client_PL_Storage_close(handle, TRUE);
	return result;
}

static VDM_Error createTmpFile(VDM_Client_PL_StorageItem_t *inStream)
{
	FILE *f;
	int i, j;
	VDM_Error result;
	char buf[32];

	memset(buf, 0, 32);
	result = VDM_Client_linux_fopen(inStream->tmpFilename, "w", &f);
	if (result != VDM_ERR_OK)
		goto end;

	for (i = 0; i < g_minFileSize; i++)
	{
		for (j = 0; j < 32; j++)
			fwrite(buf, 32, 1, f);
	}

	VDM_Client_linux_fsync(f);
	VDM_Client_linux_fclose(&f);

end:
	return result;
}

static VDM_Error openFile(VDM_Client_PL_StorageItem_t *inStream,
	E_VDM_CLIENT_PL_Storage_Access_t inMode)
{
	VDM_Error result = VDM_ERR_OK;
	struct stat buf;
	char *mode = "w";

	inStream->storageAccess = inMode;

	if (inMode == E_VDM_CLIENT_PL_Storage_Access_read)
	{
		result = VDM_Client_linux_fopen((const char *)inStream->filename, "rb",
			&inStream->file);
		if (!inStream->file)
		{
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
				("Cannot open file '%s' for reading", inStream->filename));
			result = VDM_ERR_STORAGE_OPEN;
		}
		goto end;
	}
	
	/* Open for write */
	VDM_Client_PL_assert(inMode == E_VDM_CLIENT_PL_Storage_Access_write);

	if (!inStream->tmpFilename)
	{
		/* No temporary file. open file directly */
		result = VDM_Client_linux_fopen(inStream->filename, "w",
			&inStream->file);
		if (!inStream->file)
		{
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
				("Cannot open file '%s' for writing", inStream->filename));
			result = VDM_ERR_STORAGE_OPEN;
		}
		goto end;
	}

	if (g_minFileSize)
	{
		if (!fileExists(inStream->tmpFilename))
			createTmpFile(inStream);
		mode = "r+"; /* Writing in-place: do not truncate */
	}

	result = VDM_Client_linux_fopen(inStream->tmpFilename, mode,
		&inStream->file);

	if (stat(inStream->filename, &buf) == 0)
		inStream->mode = buf.st_mode & (S_IRWXU | S_IRWXG | S_IRWXO);
	else
		inStream->mode = (mode_t)(-1);

	if (!inStream->file)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
			("Cannot open file '%s' for writing", inStream->tmpFilename));
		result = VDM_ERR_STORAGE_OPEN;
	}
end:

	return result;
}

VDM_Error VDM_Client_PL_Storage_openByName(void **outHandle, char *inFileName,
	E_VDM_CLIENT_PL_Storage_Access_t inMode)
{
	VDM_Error result = VDM_ERR_OK;
	VDM_Client_PL_StorageItem_t* stream = NULL;

	*outHandle = NULL;

	if (!inFileName)
	{
		result = VDM_ERR_BAD_INPUT;
		goto end;
	}

	stream = VDM_PL_malloc(sizeof(VDM_Client_PL_StorageItem_t));
	if (!stream)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}

	stream->tmpFilename = NULL;

	//note: cannot use VDM_UTL_strdup from Client PL!!!
	stream->filename = VDM_PL_malloc(VDM_PL_strlen(inFileName)+1);
	if (!stream->filename)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}
	VDM_PL_strcpy((char*)stream->filename, (char*)inFileName);

	stream->tmpFilename = getTmpFilenameDup(inFileName);
	if (!stream->tmpFilename)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}

	stream->mode = (mode_t)(-1);
	result = openFile(stream, inMode);
	if (result == VDM_ERR_OK)
		*outHandle = stream;

end:
	if (result!=VDM_ERR_OK && stream)
	{
		VDM_PL_freeAndNullify(stream->filename);
		VDM_PL_freeAndNullify(stream->tmpFilename);
		VDM_PL_free(stream);
	}

	return result;
}

VDM_Error VDM_Client_PL_Storage_open(void **outHandle, E_VDM_CLIENT_PL_StorageItem_t inType,
	E_VDM_CLIENT_PL_Storage_Access_t inMode)
{
	VDM_Error result = VDM_ERR_OK;
	const char* filename = NULL;
	VDM_Client_PL_StorageItem_t *stream = VDM_PL_malloc(sizeof(VDM_Client_PL_StorageItem_t));
	if (!stream)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}

	stream->tmpFilename = NULL;
	//note: cannot use VDM_UTL_strdup from Client PL!!!
	filename = (const char*)getItemFilename(inType);
	stream->filename = VDM_PL_malloc(VDM_PL_strlen(filename)+1);
	if (!stream->filename)
	{
		result = VDM_ERR_MEMORY;
		goto end;
	}
	VDM_PL_strcpy((char*)stream->filename, filename);

	result = getItemTmpFilename(inType, &(stream->tmpFilename));
	if (result != VDM_ERR_OK)
		goto end;

	stream->mode = (mode_t)(-1);
	result = openFile(stream, inMode);
	if (result == VDM_ERR_OK)
		*outHandle = stream;

end:
	if (result != VDM_ERR_OK && stream)
	{
		VDM_PL_freeAndNullify(stream->filename);
		VDM_PL_freeAndNullify(stream->tmpFilename);
		VDM_PL_free(stream);
	}

	return result;
}

UTF8Str VDM_Client_PL_Storage_fgets(void *inHandle, void *outBuffer, IU32 inBufferSize)
{
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;
	UTF8Str res = (UTF8Str)fgets((char*)outBuffer, (int)inBufferSize, 
		stream->file);

	return res;
}

VDM_Error VDM_Client_PL_Storage_read(void *inHandle, void *outBuffer, IU32 inBufSize,
	IU32 *outReadCount)
{
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;
	VDM_Error result = VDM_ERR_OK;

	if (!stream)
	{
		result = VDM_ERR_STORAGE_READ;
		goto end;
	}

	*outReadCount = (IU32)fread(outBuffer, 1, (size_t)inBufSize, stream->file);

	if (ferror(stream->file))
		result = VDM_ERR_STORAGE_READ;

end:
	return result;
}

#define MAX_WRITE_RETRY 100

VDM_Error VDM_Client_PL_Storage_write(void *inHandle, const void *inData, IU32 inLength)
{
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;
	VDM_Error result;
	IU32 retryCounter = 0;
	IU32 offset, written;
	const char *data = inData;

	if (!stream)
	{
		result = VDM_ERR_STORAGE_WRITE;
		goto end;
	}

	for (offset = 0; offset < inLength; offset += (IU32) written)
	{
		written = (IU32)fwrite(&data[offset], 1, (size_t)inLength - offset, stream->file);

		if (ferror(stream->file))
		{
			if (errno == EAGAIN || errno == EWOULDBLOCK)
			{
				usleep(10000);
				if (retryCounter++ > MAX_WRITE_RETRY)
				{
					VDM_Client_PL_log(E_VDM_LOGLEVEL_Debug,
						("vDM thread is blocked, write to file %s failed.",
						 stream->filename));
					result = VDM_ERR_STORAGE_WRITE;
					goto end;
				}
				continue;
			}
			result = VDM_ERR_STORAGE_WRITE;
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
				("Failed saving file %s: %s", stream->filename, strerror(errno)));
			goto end;
		}
	}
	result = VDM_ERR_OK;

end:
	return result;
}

VDM_Error VDM_Client_PL_Storage_close(void *inHandle, IBOOL inCommit)
{
	VDM_Error result = VDM_ERR_OK;
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;

	if (!stream)
	{
		result = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	// If the file was opened for write - sync it with the data that was written
	// to it. If sync fail - don't stop the file close process
	if (stream->storageAccess == E_VDM_CLIENT_PL_Storage_Access_write)
		VDM_Client_PL_Storage_sync(inHandle);


	if (inCommit && stream->tmpFilename && stream->mode != (mode_t)(-1) &&
		fchmod(fileno(stream->file), stream->mode))
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
				("Could not chmod %s: %s", stream->tmpFilename, strerror(errno)));
		if (errno != ENOENT)					//MERGE NOTE: is the "if" new or unnecessary?
			result = VDM_ERR_STORAGE_COMMIT;
	}
	VDM_Client_linux_fclose(&stream->file);
	if (inCommit && stream->tmpFilename)
	{
	    if (rename(stream->tmpFilename, stream->filename))
	    {
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
				("Could not rename %s: %s", stream->tmpFilename, strerror(errno)));
			//MERGE NOTE: why no "if" here"?
			result = VDM_ERR_STORAGE_COMMIT;
	    }
		else if (g_minFileSize)
		    createTmpFile(stream);
	}

	VDM_PL_freeAndNullify(stream->filename);
	VDM_PL_freeAndNullify(stream->tmpFilename);
	VDM_PL_free(stream);

end:
	return result;
}

void VDM_Client_PL_Storage_delete(E_VDM_CLIENT_PL_StorageItem_t inType)
{
	char *filename = getItemFilename(inType);
	if (filename && remove(filename) != 0)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
			("Could not delete %s: %s", filename, strerror(errno)));
	}

	//tmp file shouldn't exist, but just in case...
	if (getItemTmpFilename(inType, &filename) == VDM_ERR_OK &&
			filename && remove(filename) != 0)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Info,
			("Could not delete temp file %s: %s", filename, strerror(errno)));
	}
	VDM_PL_freeAndNullify(filename);
}

VDM_Error VDM_Client_PL_Storage_deleteByName(const char *inFileName)
{
	VDM_Error result = VDM_ERR_OK;
	char *filename = NULL;

	if (!inFileName)
	{
		result = VDM_ERR_BAD_INPUT;
		goto end;
	}

	if (remove(inFileName))
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning,
			("Could not delete %s: %s", inFileName, strerror(errno)));
		result = VDM_ERR_STORAGE_REMOVE;
	}

	//tmp file shouldn't exist, but just in case...
	filename = getTmpFilenameDup(inFileName);
	if (filename && fileExists(filename) && remove(filename))
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Info,
			("Could not delete temp file %s: %s", filename, strerror(errno)));
		result = VDM_ERR_STORAGE_REMOVE;
	}

end:
	VDM_PL_freeAndNullify(filename);
	return result;
}

VDM_Error VDM_Client_PL_Storage_fileSeek(void *inHandle, IU32 inOffset, E_VDM_CLIENT_PL_FSeek_t inSeekType)
{
	VDM_Error result = VDM_ERR_OK;
	int seekType;
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;

	switch(inSeekType)
	{
	case E_VDM_CLIENT_PL_FSeek_START:
		seekType = SEEK_SET;
		break;
	case E_VDM_CLIENT_PL_FSeek_CURRENT:
		seekType = SEEK_CUR;
		break;
	case E_VDM_CLIENT_PL_FSeek_END:
		seekType = SEEK_END;
		break;
	default:
		return -1;
	}

	if (lseek(fileno(stream->file), (off_t)inOffset, seekType)==(off_t)-1)
		result = VDM_ERR_STORAGE_SEEK;

	return result;
}

VDM_Error VDM_Client_PL_Storage_sync(void *inHandle)
{
	VDM_Client_PL_StorageItem_t* stream = (VDM_Client_PL_StorageItem_t*)inHandle;
	VDM_Error ret;

	ret = VDM_Client_linux_fsync(stream->file);
	if (ret != VDM_ERR_OK)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,("VDM_Client_PL_Storage_sync VDM_Client_linux_fsync failed, res:0x%x", ret));
		return ret;
	}

	/* If the file doesn't already exist we need to sync it's parent
	 * directory after creating it */
	if (!access(stream->filename, F_OK))
		return VDM_ERR_STORAGE_OPEN;

	/* In some cases the rename implementation might require adding here another
	 * fsync call on the registry file (E.g. Ext4/3 with pre Linux-2.6.30, where
	 * the file isn't automatically flushed after the rename) */
	if (errno == ENOENT)
	{
		FILE* dp;
		char* fileDir = ".";

		VDM_Client_PL_log(E_VDM_LOGLEVEL_Info,
			("storage: Syncing the file directory(%s)", fileDir));
		ret = VDM_Client_linux_fopen(fileDir, "r", &dp);
		if (ret == VDM_ERR_OK)
			ret = VDM_Client_linux_fsync(dp);
		if (VDM_Client_linux_fclose(&dp) || ret != VDM_ERR_OK)
		{
			VDM_Client_PL_log(E_VDM_LOGLEVEL_Error,
				("storage: Error while syncing the file directory(%s)", fileDir));
		}
	}
	return ret;
}
