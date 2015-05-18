/*
 *******************************************************************************
 *
 * GcmHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */
 
package com.redbend.swm_common;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;

/**
 * Parse GCM notification and send register request with updated sender id.
 */
public abstract class GcmHandler extends EventHandler
{
	private static final String LOG_TAG = "GcmHandler";
	
	abstract protected Class<?> getReceiverClass(); 

	public GcmHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected void genericHandler(Event ev) {
		Class<?> receiverClass = getReceiverClass();
		
		byte[] bRegId = ev.getVarStrValue("DMA_VAR_NOTIF_REG_ID");
		byte[] bSenderId = ev.getVarStrValue("DMA_VAR_SENDER_ID");
		
		Intent intent = new Intent(ctx.getApplicationContext(), receiverClass);
		
		Log.d(LOG_TAG, "recieved request from GCM BL");

		if (bSenderId != null)
			intent.putExtra(GcmReceiver.INTENT_EXTRA_SENDER_ID,	new String(bSenderId));
		if (bRegId != null)
			intent.putExtra(GcmReceiver.INTENT_EXTRA_REG_ID, new String(bRegId));
			
		if (ev.getName().equals("DMA_MSG_GCM_REGISTRATION_DATA")) {
			intent.setAction(GcmReceiver.GCM_REGISTER_REQUEST);
			Log.d(LOG_TAG, "registration request from GCM BL");
		}
		else if (ev.getName().equals("DMA_MSG_GCM_UN_REGISTRATION_DATA")) {
			intent.setAction(GcmReceiver.GCM_UNREGISTER_REQUEST);
			Log.d(LOG_TAG, "unregistration request from GCM BL");
		}
		else {
			Log.e(LOG_TAG, "undefined request requested un-registration from GCM");
			return;
			}
		ctx.sendBroadcast(intent);
	}
}
