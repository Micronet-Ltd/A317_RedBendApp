/*
 *******************************************************************************
 *
 * vdm_client_pl_linux_file.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_linux_file.c
 *
 *  \brief File Handling Utility.
 *******************************************************************************
 */

#define _FILE_OFFSET_BITS 64
#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>
#include "vdm_client_pl_linux_file.h"
#include <errno.h>

VDM_Error VDM_Client_linux_fopen(const char *inPath, const char *inMode,
	FILE **outFile)
{
	struct stat sb1, sb2;
	VDM_Error result = VDM_ERR_OK;
	int lstatResult, lstatErrno;
    
	*outFile = NULL;
	lstatResult = lstat(inPath, &sb1);
	lstatErrno = errno;
    
	*outFile = fopen(inPath, inMode);
	if (!*outFile)
	{
		//Could not open file
		result = VDM_ERR_STORAGE_OPEN;
		goto exit;
	}

	//Check for errors
	if (lstatResult == -1)
	{
		if (lstatErrno != ENOENT)
		{
			//Could not get file stat
			result = VDM_ERR_UNSPECIFIC;
			VDM_Client_linux_fclose(outFile);
		}
	}
	else if ((sb1.st_mode & S_IFMT) == S_IFLNK)
	{
		//File is a symlink (not supported)
		result = VDM_ERR_BAD_INPUT;
		VDM_Client_linux_fclose(outFile);
	}
	else if (fstat(fileno(*outFile), &sb2) == -1)
	{
		//Could not get file stat
		result = VDM_ERR_UNSPECIFIC;
		VDM_Client_linux_fclose(outFile);
	}
	else if (sb1.st_dev != sb2.st_dev || sb1.st_ino != sb2.st_ino)
	{
		//file switched between the fopen call and the first lstat call
		result = VDM_ERR_BAD_INPUT;
		VDM_Client_linux_fclose(outFile);
	}

exit:
	return result;
}

VDM_Error VDM_Client_linux_fclose(FILE **outFile)
{
	VDM_Error result = VDM_ERR_OK;

	if (*outFile && fclose(*outFile))
		result = VDM_ERR_UNSPECIFIC;

	*outFile = NULL;
		
	return result;
}

VDM_Error VDM_Client_linux_fsync(FILE *inFile)
{
	VDM_Error result = VDM_ERR_OK;
	int fd;
	if (fflush(inFile))
	{
		if (errno != EBADF) // EBADF Stream is not an open stream, or is not open for writing.
		{
			result = VDM_ERR_UNSPECIFIC;
			goto end;
		}
	}

	fd = fileno(inFile);
	if (fd == -1)
	{
		result = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	if (fsync(fd))
		result = VDM_ERR_UNSPECIFIC;
end:
	return 	result;
}

VDM_Error VDM_Client_linux_rename(const char *inOldpath, const char *inNewpath)
{
	VDM_Error result = VDM_ERR_OK;
	int ret = rename(inOldpath, inNewpath);

	if (ret == -1)
		result = VDM_ERR_UNSPECIFIC;

	return result;
}

