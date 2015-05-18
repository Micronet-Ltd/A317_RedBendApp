/*
 *******************************************************************************
 *
 * RBGcmReceiver.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
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
