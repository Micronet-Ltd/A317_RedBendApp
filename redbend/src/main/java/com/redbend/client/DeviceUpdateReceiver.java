/*
 *******************************************************************************
 *
 * DeviceUpdateReceiver.java
 *
 * Handles OEM settings of device update commands:
 * - Enable, disable, or change the periodic check for updates
 * - Check for updates now
 *
 * Sends BL events:
 * DMA_MSG_SCOMO_DEVINIT_POLLING_ENABLE
 * DMA_MSG_SCOMO_DEVINIT_POLLING_DISABLE
 * DMA_MSG_SCOMO_DEVINIT_POLLING_CHANGE
 * D2B_CLIENT_STARTED
 * DMA_MSG_SESS_INITIATOR_USER_SCOMO
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmReceive;

/**
 * Handle intents comming from OEM regarding device update
 */
public class DeviceUpdateReceiver extends SmmReceive {
	
	public static final String INTENT_PREFIX = "SwmClient.";
	private static final String ENABLE_PERIODIC_CHECK_FOR_UPDATES = INTENT_PREFIX + "ENABLE_PERIODIC_CHECK_FOR_UPDATES";
	private static final String DISABLE_PERIODIC_CHECK_FOR_UPDATES = INTENT_PREFIX + "DISABLE_PERIODIC_CHECK_FOR_UPDATES";
	private static final String CHANGE_PERIODIC_CHECK_FOR_UPDATES = INTENT_PREFIX + "CHANGE_PERIODIC_CHECK_FOR_UPDATES";
	public static final String CHECK_FOR_UPDATES_NOW = INTENT_PREFIX + "CHECK_FOR_UPDATES_NOW";
	private static final String START_APPLICATION = INTENT_PREFIX + "START_APPLICATION";
	private static final String KEY_INTERVAL = "interval";
	private static final int INVALID_VALUE = -1;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (action == null) {
			Log.d(LOG_TAG, "No action found!");
			return;
		}
		
		Log.d(LOG_TAG, "onReceive action:" + action);
		if (!action.equals(ENABLE_PERIODIC_CHECK_FOR_UPDATES) &&
			!action.equals(DISABLE_PERIODIC_CHECK_FOR_UPDATES) &&
			!action.equals(CHANGE_PERIODIC_CHECK_FOR_UPDATES) &&
			!action.equals(CHECK_FOR_UPDATES_NOW) &&
			!action.equals(START_APPLICATION))
			return;
		Log.d(LOG_TAG, "onReceive: "+ action);

		Event event = null ;
		if ( action.equals(ENABLE_PERIODIC_CHECK_FOR_UPDATES) )
			event = new Event("DMA_MSG_SCOMO_DEVINIT_POLLING_ENABLE");
		else if ( action.equals(DISABLE_PERIODIC_CHECK_FOR_UPDATES) )
			event = new Event("DMA_MSG_SCOMO_DEVINIT_POLLING_DISABLE");
		else if ( action.equals(CHANGE_PERIODIC_CHECK_FOR_UPDATES) ){
			event = new Event("DMA_MSG_SCOMO_DEVINIT_POLLING_CHANGE");
			event.addVar(new EventVar("DMA_VAR_SCOMO_DEVINIT_NEW_POLLING_INTERVAL", intent.getIntExtra(KEY_INTERVAL, INVALID_VALUE)));
		}
		else if ( action.equals(START_APPLICATION) ){
			event = new Event("D2B_CLIENT_STARTED");
		}
		else if ( action.equals(CHECK_FOR_UPDATES_NOW) ){		
			Resources res = context.getResources();
			Intent startupActivityIntent;



			if (res.getBoolean(R.bool.isAutomotive))
				startupActivityIntent = new Intent(context, StartupActivityAutomotive.class);
			else
				startupActivityIntent = new Intent(context, StartupActivity.class);

			startupActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startupActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			context.startActivity(startupActivityIntent);
			return;
		}


		if (event != null)
			sendEvent(context, ClientService.class,event);
	}
}
