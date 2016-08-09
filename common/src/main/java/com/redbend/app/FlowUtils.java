/*
 *******************************************************************************
 *
 * FlowUtils.java
 *
 * Utilities (such as logging) to support FlowManager.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;
import android.text.format.Time;
import android.util.Log;

public class FlowUtils {
	private static StringBuilder logStr = new StringBuilder(255);
		
	public final static void dLog(String tag, int flowId, CharSequence text) {
		logStr.setLength(0);
		logStr.append(text);
		logStr.append(", flow= ");
		logStr.append(Integer.toString(flowId));
		Log.d(tag, logStr.toString());
	}
	
	// TODO - this function is used by UpdateStatus until TT 1236 is fixed
	public static String getCurrentTime()
	{
		Time time = new Time();
		time.setToNow();
		return time.format3339(false);
		//TODO: show only the date: return time.format3339(true);
	}
}

