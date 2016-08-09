/*
 *******************************************************************************
 *
 * SmmConstants.java
 *
 * Sets some SMM constants.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

/** 
 * Specify certain SMM constants.
 */
public final class SmmConstants {

	/* DMA_VAR_SCOMO_TRIGGER_MODE */
	public final static int SCOMO_MODE_SERVER = 0;
	public final static int SCOMO_MODE_DEVICE = 1;
	public final static int SCOMO_MODE_USER	= 3;

	/* Relevant to Roaming, RTT VDO (coverage), EMERGENCY MODE */
	public final static int FALSE =	0;
	public final static int TRUE =	1;
}
