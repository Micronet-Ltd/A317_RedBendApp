/*
 *******************************************************************************
 *
 * SmmCommonConstants.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

/** 
 * Specify certain SMM constants.
 */
public final class SmmCommonConstants {
	/* DESCMO result codes */ 
	public final static int DESCMO_OPERATION_SUCCESS = 0;
	public final static int DESCMO_OPERATION_CANCELED = 1;
	public final static int DESCMO_OPERATION_FAILED = 2;

	/* Keep in sync with the C enum E_DP_Type_t */
	public final static int E_DP_Type_Scomo = 0;
	public final static int E_DP_Type_Fumo = 1;
	public final static int E_DP_Type_FumoInScomo = 2;
	public final static int E_DP_Type_Descmo = 3;
}
