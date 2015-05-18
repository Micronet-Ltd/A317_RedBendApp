/*
 *******************************************************************************
 *
 * vdm_swmc_pl_dir.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_swmc_pl_dir.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <dirent.h>
#include <errno.h>

#include <vdm_components.h>
#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_pl_string.h>
#include <swm_general_errors.h>
#include <vdm_swmc_pl_dir.h>
#include <vdm_utl_logger.h>
#include <vdm_utl_calloc.h>
#include <vdm_utl_utf8.h>

#define VDM_COMPONENT_ID	(E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC

typedef struct {
	UTF8Str name;
} VDM_SWMC_PL_File_t;

typedef struct {
	VDM_SWMC_PL_File_t* files;
	IU32 filesCount;
	IU32 filesCurIndex;
} VDM_SWMC_PL_Dir_t;

static IBOOL isDir(const char *inPath, const char *inFile)
{
	struct stat buf;
	char *fullFileName;
	IBOOL ret = FALSE;

	/* +1 for null termination, +1 for dir separator ("/") */
	fullFileName = VDM_PL_malloc(VDM_UTL_utf8len(inFile) +
		VDM_UTL_utf8len(inPath) + 2);
	if (!fullFileName)
		goto end;

	/* Build file name with full path. */
	VDM_UTL_utf8cpy(fullFileName, inPath);
	VDM_UTL_utf8cat(fullFileName, "/") ;
	VDM_UTL_utf8cat(fullFileName, inFile);

	if (stat((const char *)fullFileName, &buf))
	{
		VDM_logError(("Error, can't stat %s - %s", inFile, strerror(errno)));
		goto end;
	}
	ret = S_ISDIR(buf.st_mode);

end:
	if (fullFileName)
		VDM_PL_free(fullFileName);
	return ret;
}

SWM_Error VDM_SWMC_PL_Dir_create(void** outHandle, UTF8CStr inPath)
{
	SWM_Error result = SWM_ERR_DIR_OPEN;
	IU32 i;
	DIR *dirp = NULL;
	struct dirent* dirEntry = NULL;

	VDM_SWMC_PL_Dir_t* instance = VDM_UTL_calloc(sizeof(VDM_SWMC_PL_Dir_t));
	if(!instance)
	{
		result = SWM_ERR_MEMORY;
		goto end;
	}

	dirp = opendir((char*)inPath);
	if(!dirp)
	{
		VDM_logError(("VDM_SWMC_PL_getFirstFile: opendir() failed - %s\n", strerror(errno)));
		goto end;
	}

	// read all files to global array
	while ((dirEntry = readdir(dirp)))
	{
		if (isDir((const char*)inPath, dirEntry->d_name))
			continue;
		instance->filesCount++;
	}

	instance->files = VDM_UTL_calloc(instance->filesCount * sizeof(VDM_SWMC_PL_File_t));
	if(!instance->files)
	{
		result = SWM_ERR_MEMORY;
		goto end;
	}

	rewinddir(dirp);

	for (i = 0; (dirEntry = readdir(dirp)) && i < instance->filesCount; )
	{

		if (isDir((const char*)inPath, dirEntry->d_name))
			continue;

		instance->files[i].name = VDM_UTL_utf8dup(dirEntry->d_name);
		if (!instance->files[i].name)
		{
			result = SWM_ERR_MEMORY;
			goto end;
		}
		i++;
	}

	result = SWM_ERR_OK;
end:
	if (dirp)
		closedir(dirp);
	if (result != SWM_ERR_OK)
		VDM_SWMC_PL_Dir_destroy(instance);
	*outHandle = instance;
	return result;

}

SWM_Error VDM_SWMC_PL_Dir_getNextFile(void *inHandle, UTF8Str outBuffer,
	IU32 *ioBufferLen)
{
	SWM_Error result = SWM_ERR_BAD_INPUT;
	VDM_SWMC_PL_Dir_t *instance = (VDM_SWMC_PL_Dir_t*)inHandle;
	UTF8Str name = (UTF8Str)instance->files[instance->filesCurIndex].name;
	IU32 filenameLen = 0;

	if(!instance)
	{
		goto end;
	}

	if(instance->filesCurIndex == instance->filesCount)
	{
		result = SWM_ERR_OK;
		goto end;
	}

	instance = (VDM_SWMC_PL_Dir_t*)inHandle;
	filenameLen = VDM_UTL_utf8len(name);

	if(filenameLen > *ioBufferLen)
	{
		result = SWM_ERR_BUFFER_OVERFLOW;
		goto end;
	}

	VDM_UTL_utf8cpy(outBuffer, name);

	instance->filesCurIndex++;

	result = SWM_ERR_OK;
end:
	*ioBufferLen = filenameLen;
	return result;
}

void VDM_SWMC_PL_Dir_destroy(void* inHandle)
{
	IU32 i = 0;
	VDM_SWMC_PL_Dir_t* instance = NULL;
	if(!instance)
	{
		goto end;
	}

	instance = (VDM_SWMC_PL_Dir_t*)inHandle;
	for(i=0; i<instance->filesCount; i++)
	{
		VDM_PL_freeAndNullify(instance->files[i].name);
	}

	VDM_PL_free(instance->files);
	VDM_PL_free(instance);

end:
	return;
}
