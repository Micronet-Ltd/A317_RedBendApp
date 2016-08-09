/*
 *******************************************************************************
 *
 * WipeAgentHandler.java
 *
 * Wipes device (LAWMO).
 *
 * Receives DIL events:
 * DMA_MSG_LAWMO_WIPE_AGENT_LAUNCH
 *
 * Sends BL events:
 * DMA_MSG_LAWMO_WIPE_AGENT_ENDED_FAILURE
 * DMA_MSG_LAWMO_WIPE_AGENT_ENDED_SUCCESS
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.app.Event;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Log;

/**
 * Wipe device (LAWMO)
 */
public class WipeAgentHandler extends LawmoHandlerBase {
	private static final String CLASS_NAME = "WipeAgentHandler";
	public WipeAgentHandler(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected void handleEvent(Event ev) {
		
	}
	
	@Override
	protected int tryOperation(Context context) throws SecurityException {
		Log.d(CLASS_NAME,  CLASS_NAME + ".tryOperation");

		_dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
		Log.e(CLASS_NAME,  CLASS_NAME + ".tryOperation wipe failed");
		return 0; // return fail as after wipe command the device should reset so this code is not really reachable.
	}

	@Override
	protected String getResultStringForDebug(int result) {
		if (result == 1) {
			return "unsupported result for wipe:" + result;
		}
		return "Wipe failed";
	}

	@Override
	protected String getEvent(int result) {
		if (result == 1)
			Log.e(CLASS_NAME, " getEvent - unsupported result: " + result); // We currently implement wipe as factory reset, so
															//once wipe operation occurred the device will reset and successful wipe
															//event will never be sent to the engine.
		return "DMA_MSG_LAWMO_WIPE_AGENT_ENDED_FAILURE";
	}
}
