/*
 *******************************************************************************
 *
 * vdm_swmc_pl_ua.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_swmc_pl_ua.h
 *
 * \brief	Update Agent API
 *******************************************************************************
 */

#ifndef VDM_SWMC_PL_UA_H_
#define VDM_SWMC_PL_UA_H_

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Set the Update Agent DP path.
 *
 * \param	inDpPath		Absolute path to DP file.
 * \param	inDpPathFile	File in which to store \a inDpPath.
 * \param	inHandoffDir	Directory in which to store \a inDpPathFile.
 *
 * \return	SWM_ERR_OK on success, or an \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_UA_handoff(char *inDpPath, char *inDpPathFile, UTF8CStr inHandoffDir);

/**
 * Get Update Agent update result. Remove DP path file and result file.
 *
 * \param	outUpdateResult Pointer to the update result.
 * \param	inResultFile	File in which the result is stored.
 * \param	inDpPathFile	File in which the DP path is stored.
 *
 * \return	SWM_ERR_OK on success, or an \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_UA_getResult(IU32* outUpdateResult, char *inResultFile, char *inDpPathFile);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* VDM_SWMC_PL_DEVICE_H_ */
