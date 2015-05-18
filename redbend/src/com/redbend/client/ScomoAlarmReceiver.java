/*
 *******************************************************************************
 *
 * ScomoAlarmReceiver.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.app.Event;
import com.redbend.app.SmmReceive;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** 
 * Listen for polling alarm set by \ref ScomoAlarmSetter.
 */
public class ScomoAlarmReceiver extends SmmReceive { 
    @Override
    public void onReceive(Context context, Intent intent) {
        
		Event event = new Event("DMA_MSG_DL_TIMESLOT_TIMEOUT");
		Log.v("ScomoAlarmReceiver", "Scomo alarm expired, sending event " + event.getName());
		sendEvent(context, ClientService.class, event);
    }
}
