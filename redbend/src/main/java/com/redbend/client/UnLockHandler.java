/*
 *******************************************************************************
 *
 * UnLockHandler.java
 *
 * Unlocks device (LAWMO).
 *
 * Receives DIL events:
 * DMA_MSG_LAWMO_UNLOCK_LAUNCH
 *
 * Sends BL events:
 * DMA_MSG_LAWMO_UNLOCK_ENDED_FAILURE
 * DMA_MSG_LAWMO_UNLOCK_ENDED_SUCCESS
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;

import com.redbend.app.Event;

import android.content.Context;
import android.util.Log;

/** 
 * Unlock device (LAWMO).
 */ 
public class UnLockHandler extends LawmoHandlerBase {
	
	private static final String LOG_TAG = "UnLockHandler";
	protected boolean m_pwdFromServer;

	public UnLockHandler(Context ctx) {
		super(ctx);
		m_pwdFromServer = false;
	}
	
	@Override
	protected void handleEvent(Event ev) {
		try{
			if (ev.getVarValue("DMA_VAR_LAWMO_IS_RESET_PWD") == 0)
				m_pwdFromServer = true;
		}catch(Exception e){
			Log.d(LOG_TAG, "DMA_VAR_LAWMO_IS_RESET_PWD has not send in the event");
		}
	}
	
	@Override
	protected int tryOperation(Context context) throws SecurityException {
		int result = 0;
		File reg = context.getFileStreamPath(passwordSet);
		
		// Check if the dma app locked the device
		if (reg.exists()) 
		{
			reg.delete();

			if ((!m_pwdFromServer) && (_dpm.resetPassword("", 0))) {
				result = 1;
				_dpm.lockNow();
			}
			else if (m_pwdFromServer){
				result=1;
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
