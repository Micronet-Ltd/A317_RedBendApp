/*
 *******************************************************************************
 *
 * vdm_swmc_pl_dp.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_swmc_pl_dp.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <vdm_components.h>
#include <vdm_swmc_pl_dp.h>
#include <vdm_utl_logger.h>

#define VDM_COMPONENT_ID	(E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC

#define DP_SIGNATURE_SIZE 0

SWM_Error VDM_SWMC_PL_Dp_validateExternalSignatureDp(const char *inPath, IU32 *outOffset)
{
	SWM_Error result = SWM_ERR_OK;
	//result = SWM_ERR_DP_EXT_VALIDATION_FAILED;

	// Do validation of client specific DP signature here...
	
	*outOffset =  DP_SIGNATURE_SIZE;
	return result;
}

