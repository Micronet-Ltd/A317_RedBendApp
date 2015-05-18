/*
 *******************************************************************************
 *
 * SmmReceive.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.IOException;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.content.Context;
import android.content.Intent;

abstract public class SmmReceive extends WakefulBroadcastReceiver {
	protected final String LOG_TAG = getClass().getSimpleName();
	
	protected static void sendEvent(Context context, Class<?> serviceCls, Event ev)
	{
		try {
			sendData(context, serviceCls, SmmService.startServiceMsgExtra,
					ev.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	/* XXX there should be no need for such flags, everything must be passed
	 * through the SM, right now this is needed for timer persistence, which
	 * indeed will be moved later to the State Machine */
	protected static void setFlag(Context context, Class<?> serviceCls, String name)
	{
		sendData(context, serviceCls, name, null);
	}

	private static void sendData(Context context, Class<?> serviceCls, String name,
			byte[] value)
	{
		Intent i = new Intent(context.getApplicationContext(), serviceCls);
		if (value == null)
			i.putExtra(name, true);
		else
			i.putExtra(name, value);
		i.putExtra(SmmService.startServiceFromSmmReceiveExtra, true);
		startWakefulService(context, i);
	}
}
