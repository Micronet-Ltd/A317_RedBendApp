/*
 *******************************************************************************
 *
 * vdm_client_pl_global.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_global.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <vdm_pl_types.h>
#include <vdm_client_pl_global.h>
#include <vdm_pl_alloc.h>

static void* s_vdmData = NULL;

//---------------------------------
//	VDM_Client_PL_Global_set
//---------------------------------
IBOOL VDM_Client_PL_Global_set(void* inData)
{
	IBOOL result = FALSE;

	if (!s_vdmData)
	{
		s_vdmData = inData;
		result = TRUE;
	}
	return result;
}

//---------------------------------
//	VDM_Client_PL_Global_get
//---------------------------------
void* VDM_Client_PL_Global_get(void)
{
	return s_vdmData;
}

//---------------------------------
//	VDM_Client_PL_Global_reset
//---------------------------------
void VDM_Client_PL_Global_reset(void)
{
	s_vdmData = NULL;
}

