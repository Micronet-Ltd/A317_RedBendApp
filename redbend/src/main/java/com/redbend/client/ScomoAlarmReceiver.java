/*
 *******************************************************************************
 *
 * ScomoAlarmReceiver.java
 *
 * Listens for polling alarm set by ScomoAlarmSetter.
 *
 * Sends BL events:
 * DMA_MSG_DL_TIMESLOT_TIMEOUT: when a polling alarm expires
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
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
