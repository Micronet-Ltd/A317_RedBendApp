/*
 *******************************************************************************
 *
 * GetBatteryLevelHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */
 
 package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.EventVar;

/**
 * Get battery level and send event of checking battery level.
 */
public class GetBatteryLevelHandler extends EventHandler
{
	private static final String LOG_TAG = "GetBatteryLevelHandler";

	public GetBatteryLevelHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected void genericHandler(Event ev) {

		try {			
			int user = ev.getVar("DMA_VAR_INITIATOR_USER").getValue();
			if(user == SmmConstants.TRUE)
				// In foreground if application is launched from menu icon
				((ClientService) ctx).startFlowInForeground(1);
			else
				// In background if application is NOT launched from menu icon and no activity exists
				((ClientService) ctx).startFlowInBackground(1);
			
		} catch (Exception e) {
			Log.e(LOG_TAG, "DMA_VAR_INITIATOR_USER is missing");
		}
		
		int batteryLevel = getBatteryLevel();
		
		Event event = new Event("DMA_MSG_BATTERY_LEVEL")
			.addVar(new EventVar("DMA_VAR_BATTERY_LEVEL", batteryLevel));
		
		Log.d(LOG_TAG, "Sending event " + event.getName());
		((ClientService)ctx).sendEvent(event);
	}
	
	private int getBatteryLevel() {
	    Intent battery = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); 
	    
	    if(battery == null)
	    	return 0;
	    
	    int level = battery.getIntExtra("level", 0); 
	    int scale = battery.getIntExtra("scale", 100);
	    
	    if(scale == 0)
	    	return 0;
	    	
	    int ret = level * 100 / scale;
	    
	    Log.i(LOG_TAG, "Current Battery Level: " + ret);
	    return ret;
	}
}
