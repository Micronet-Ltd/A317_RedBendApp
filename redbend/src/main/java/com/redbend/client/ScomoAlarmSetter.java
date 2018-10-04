/*
 *******************************************************************************
 *
 * ScomoAlarmSetter.java
 *
 * Sets a polling alarm to be handled by ScomoAlarmReceiver. The alarm time is
 * set in DMA_VAR_SCOMO_DOWNLOAD_TIME_SECONDS.
 * 
 * Receives DIL events:
 * DMA_MSG_SCOMO_SET_DL_TIMESLOT
 *
 * Sends BL events:
 * DMA_MSG_SCOMO_SET_DL_TIMESLOT_DONE: when a polling alarm is set.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.util.Calendar;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** 
 * Set a polling alarm to be handled by \ref ScomoAlarmReceiver.
 */
public class ScomoAlarmSetter extends EventHandler {

	protected final String LOG_TAG = "ScomoAlarmSetter";
	
	private PendingIntent pendingIntent;
		 
	public ScomoAlarmSetter(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected void genericHandler(Event ev) {
		Log.d(LOG_TAG, "ScomoAlarmSetter started");
				
		long alarmTimeInSeconds = -1;
		String eventName = ev.getName();
		
		if (!eventName.equals("DMA_MSG_SCOMO_SET_DL_TIMESLOT")) {
			return;
		}
		alarmTimeInSeconds = ev.getVarValue("DMA_VAR_SCOMO_DOWNLOAD_TIME_SECONDS");

		Intent intent = new Intent(ctx, ScomoAlarmReceiver.class);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            
		AlarmManager alarmManager = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
		
		//first, cancel previous alarm
		Log.d(LOG_TAG, "ScomoAlarmSetter: canceling previous alarm");
		alarmManager.cancel(pendingIntent);
		

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(1000*alarmTimeInSeconds);
		Log.d(LOG_TAG, "ScomoAlarmSetter: Setting alarm for " + calendar.getTime());
		alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
		
		Event event = new Event("DMA_MSG_SCOMO_SET_DL_TIMESLOT_DONE");
	
		Log.d(LOG_TAG, "Sending event " + event.getName());
		((ClientService)ctx).sendEvent(event);
	}
}
