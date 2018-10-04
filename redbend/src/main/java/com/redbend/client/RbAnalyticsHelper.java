/*
 *******************************************************************************
 *
 * RbAnalyticsHelper.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

public class RbAnalyticsHelper {
	
	public static final String START_STOP_VSENSE_INTENT_FILTER = "com.redbend.client.StopStartService";
	public static final String STOP_VSENSE_SERVICE_EXTRA_DATA = "stop_service";
	public static final String ANALYTICS_STATUS_PREFS = "analytics_status_prefs";
	public static final String ANALYTICS_SERVICE_RUNNING_KEY = "analytics_service_running";
	public static final String LOG_TAG = "RbAnalyticsHelper";
	
	public static boolean isRbAnalyticsDelivery(Context ctx)
	{
		Resources res = ctx.getResources();
		return res.getBoolean(R.bool.buildRbAnalytics);
	}
	
	public static boolean isRbAnaliticsRunning(Context ctx)
	{
		SharedPreferences settings = ctx.getSharedPreferences(ANALYTICS_STATUS_PREFS, 0);
		return settings.getBoolean(ANALYTICS_SERVICE_RUNNING_KEY, true);
	}
	
	public static void setRbAnalyticsServiceState(Context ctx, boolean enabled)
	{
		SharedPreferences settings = ctx.getSharedPreferences(ANALYTICS_STATUS_PREFS, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(ANALYTICS_SERVICE_RUNNING_KEY, enabled);
		editor.commit();
		
		//send stop/start Analytics service intent
		boolean stopService = !enabled;
		Intent intent = new Intent(START_STOP_VSENSE_INTENT_FILTER);
		intent.putExtra(STOP_VSENSE_SERVICE_EXTRA_DATA, stopService);
		ctx.sendBroadcast(intent);
		Log.d(LOG_TAG,"-setRbAnalyticsServiceState");
	}
}
