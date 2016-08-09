/*
 *******************************************************************************
 *
 * RBGcmReceiver.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.swm_common.GcmReceiver;

public class RBGcmReceiver extends GcmReceiver{
	
	@Override
	protected Class<?> getServiceClass(){
		return ClientService.class;
	}
}
