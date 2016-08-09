/*
 *******************************************************************************
 *
 * IntentBroadcaster.java
 *
 * Sends intents.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_CONFIRM_UI
 * DMA_MSG_SCOMO_NOTIFY_DL_UI
 * DMA_MSG_SCOMO_INSTALL_DONE
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;

import android.content.Intent;
import android.content.Context;
import android.util.Log;

public class IntentBroadcaster extends EventHandler
{
	// handling SWM client intents
	public static final String TAG = "SwmcReceiver";
	
	private static final String PACKAGE_AVAILBLE_ACTIVITY = "DMA_MSG_SCOMO_DL_CONFIRM_UI";
	private static final String PACKAGE_AVAILBLE_NOTOFICATION = "DMA_MSG_SCOMO_NOTIFY_DL_UI";
	
	private static final String NEW_UPDATE_AVAILABLE = DeviceUpdateReceiver.INTENT_PREFIX + "NEW_UPDATE_AVAILABLE";
	private static final String UPDATE_SESSION_END = DeviceUpdateReceiver.INTENT_PREFIX + "UPDATE_SESSION_END";
		
	public IntentBroadcaster(Context ctx) {
		super(ctx);
	}

	private final static String LOG_TAG = "IntentBrodcaster";

	private static Intent preIntent(String eventName)
	{
		Intent intent = new Intent();
		String intentName = null;
		if (eventName.equals(PACKAGE_AVAILBLE_ACTIVITY) || eventName.equals(PACKAGE_AVAILBLE_NOTOFICATION))
			intentName = NEW_UPDATE_AVAILABLE;
		else
			intentName = UPDATE_SESSION_END;
		
       	intent.setAction(intentName);
       	Log.d(LOG_TAG, "Broadcasts Intent: " + intentName);
       	return intent;
	}
	
	@Override
	protected void genericHandler(Event ev) {
		Intent intent = preIntent(ev.getName()); 
		ctx.sendBroadcast(intent);
	}
}
