/*
 *******************************************************************************
 *
 * vdm_client_pl_linux_file.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_linux_file.h
 *
 * \brief IO Management APIs
 *******************************************************************************
 */
#ifndef _VDM_CLIENT_PL_LINUX_FILE_H_
#define _VDM_CLIENT_PL_LINUX_FILE_H_

#ifdef __cplusplus
extern "C" {
#endif

/*!
*******************************************************************************
 * Open a file ,check it is not a symbolic link and return a file pointer to it 
 *
 * \param	inPath			The path of the requested file.
 * \param	inMode			A string with configuration for the fopen command.
 * \note					If other platforms are included this might need to
 * 							be changed to a specific parameter per file
 * 							permission flag.
 * \param	outFile			A reference to the file pointer, in which the file
 * 							pointer will be returned upon a successful call.
 *
 * \return	VDM_ERR_OK upon a successful call,
 *			VDM_ERR_UNSPECIFIC if the type of the file could not be retrieved
 *			or VDM_ERR_BAD_INPUT if the file is a symbolic link.
*******************************************************************************
 */
VDM_Error VDM_Client_linux_fopen(
		const char *path,
		const char *mode,
		FILE **outFile);

/*!
*******************************************************************************
 * Close a file pointed to by a file pointer (if it is not NULL), and set the
 * file pointer to null if successful.
 *
 * \param	outFile			A file pointer to the file.
 *
 * \return	VDM_ERR_OK upon a successful call,
 *			or VDM_ERR_UNSPECIFIC if the file couldn't be closed.
*******************************************************************************
 */
VDM_Error VDM_Client_linux_fclose(FILE **outFlile);

/*!
*******************************************************************************
 * Sync the file (write all data that is still not written).
 * parent directory is also suggested.
 *
 * \param	inFile			A file pointer to the file.
 *
 * \return	VDM_ERR_OK upon a successful call,
 *			or VDM_ERR_UNSPECIFIC if the file couldn't be synced.
*******************************************************************************
 */
VDM_Error VDM_Client_linux_fsync(FILE *inFile);

/*!
*******************************************************************************
 * Rename a file. If a file already exist with the new URI it is replaced.
 * Notice: Some file system (e.g. Ext4/3 with pre Linux-2.6.30) might also
 * require a sync to flush the file's data to disc.
 *
 *
 * \return	VDM_ERR_OK upon a successful call,
 *			or VDM_ERR_UNSPECIFIC if the file couldn't be renamed for any reason.
*******************************************************************************
 */
VDM_Error VDM_Client_linux_rename(const char *oldpath, const char *newpath);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

