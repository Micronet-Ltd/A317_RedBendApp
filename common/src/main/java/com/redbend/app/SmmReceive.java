/*
 *******************************************************************************
 *
 * SmmReceive.java
 *
 * Logic to send BL events to the BLL. An Android broadcast receiver should
 * inherit this class.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import android.support.v4.content.WakefulBroadcastReceiver;
import android.content.Context;

abstract public class SmmReceive extends WakefulBroadcastReceiver {
	protected final String LOG_TAG = getClass().getSimpleName();
	
	protected static void sendEvent(Context context, Class<?> serviceCls, Event ev){
		SmmClient.sendEventToSmmService(context, serviceCls, ev);
	}
	
	protected static void setFlag(Context context, Class<?> serviceCls, String name){
		SmmClient.setFlag(context, serviceCls, name);
	}
}
