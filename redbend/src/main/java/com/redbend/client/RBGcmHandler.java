/*
 *******************************************************************************
 *
 * RBGcmHandler.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.swm_common.GcmHandler;
import android.content.Context;

public class RBGcmHandler extends GcmHandler{
	
	public RBGcmHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected Class<?> getReceiverClass(){
		return RBGcmReceiver.class;
	}
}
