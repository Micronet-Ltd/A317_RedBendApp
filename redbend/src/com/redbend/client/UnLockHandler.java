/*
 *******************************************************************************
 *
 * UnLockHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;

import android.content.Context;
import android.util.Log;

/** 
 * Unlock device (LAWMO).
 */ 
public class UnLockHandler extends LawmoHandlerBase {

	private String CLASS_NAME = this.getClass().getSimpleName();
	
	public UnLockHandler(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected int tryOperation(Context context) throws SecurityException {
		int result = 0;
		File reg = context.getFileStreamPath(passwordSet);
		
		// Check if the dma app locked the device
		if (reg.exists()) {
			if (_dpm.resetPassword("", 0)) {
				if (!reg.delete())
					Log.w(CLASS_NAME, CLASS_NAME + ".shouldLock:" + "passwordSet is not deleted");	
				result = 1;
			}
		}	
		else {
			// Screen lock password configured by user (no previous successful locking operation
			// by this application), do nothing
			result = 2;
		}
		return result;
	}
	
	@Override
	protected String getResultStringForDebug(int result) {
		if (result == 0)
			return "Reset password failed";
		else if (result == 2)
			return "Device wasn't locked before";
		// result == 1
		return "Reset password succeded";
	}
	
	@Override
	protected String getEvent(int result) {
		if (result == 1)
			return "DMA_MSG_LAWMO_UNLOCK_ENDED_SUCCESS";
		return "DMA_MSG_LAWMO_UNLOCK_ENDED_FAILURE";
		
	}
}
