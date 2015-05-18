/*
 *******************************************************************************
 *
 * vdm_swmc_pl_dp.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_swmc_pl_dp.h
 *
 * \brief	DP API
 *******************************************************************************
 */

#ifndef VDM_SWMC_PL_DP_H_
#define VDM_SWMC_PL_DP_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <swm_general_errors.h>

/**
 * Validate DP signature.
 *
 * \param	inPath		Path to DP file
 * \param	outOffset 	Offset to the actual DP start
 *
 * \return	SWM_ERR_OK on success, else SWM_ERR_DP_EXT_VALIDATION_FAILED
 */
SWM_Error VDM_SWMC_PL_Dp_validateExternalSignatureDp(const char *inPath, IU32 *outOffset);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* VDM_SWMC_PL_DP_H_ */
