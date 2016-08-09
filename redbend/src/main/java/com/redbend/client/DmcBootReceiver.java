/*
 *******************************************************************************
 *
 * DmcBootReceiver.java
 *
 * Handles power on.
 *
 * Sends BL events:
 * DMA_MSG_STS_POWERED: when the device powers on
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.SmmReceive;
import com.redbend.app.SmmService;

/**
 * Handle power on.
 */
public class DmcBootReceiver extends SmmReceive
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i(LOG_TAG, "onReceive DmcBootReceiver sending DMA_MSG_STS_POWERED to ClientService");
		sendEvent(context, ClientService.class, new Event("DMA_MSG_STS_POWERED"));
		setFlag(context, ClientService.class, SmmService.deviceBooted);
	}
}
