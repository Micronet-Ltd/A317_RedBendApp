/*
 *******************************************************************************
 *
 * vdm_swmc_pl_dir.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_swmc_pl_dir.h
 *
 * \brief	File API
 *******************************************************************************
 */

#ifndef VDM_SWMC_PL_DIR_H_
#define VDM_SWMC_PL_DIR_H_

#ifdef __cplusplus
extern "C" {
#endif

/**
 *	The function searches a directory for a file whose name matches the specified file name
 *
 * \param outHandle			****
 * \param inPath 			Pointer to a null-terminated string which specifies
 * 							a valid directory or path and file name <br>
 *							which can contain wildcard characters,
 *							such as an asterisk (*) or a question mark (?)
 *
 * \return SWM_ERR_OK if succeeded, or a \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_Dir_create(void** outHandle, UTF8CStr inPath);

/**
 *	Gets next file name
 *
 * \param inHandle 			A search handle
 * \param outBuffer 		Pre-allocated buffer where the result file will be filled
 * \param ioBufferLen 		Input:Length of outBuffer, Output: actual id length.
 *
 *
 * \return SWM_ERR_OK if succeeded, or a \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_Dir_getNextFile(void* inHandle, UTF8Str outBuffer,
		IU32* ioBufferLen);

/**
 *	Closes a file search handle opened by the VDM_SWMC_PL_Dir_create and frees memory
 *
 * \param  inHandle 	The file search handle
 *
 */
void VDM_SWMC_PL_Dir_destroy(void* inHandle);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* VDM_SWMC_PL_DIR_H_ */
