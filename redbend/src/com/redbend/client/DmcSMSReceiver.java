/*
 *******************************************************************************
 *
 * DmcSMSReceiver.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmReceive;

/**
 * Simulate a WAP Push.
 *
 * \note	This class (and its AndroidManifest.XML declaration) should be
 *			removed from a production system.
 */
public class DmcSMSReceiver extends SmmReceive
{
	private static final String WAP_PREFIX = "WAPPUSH "; 
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String nia;
		Object messages[] = (Object[])intent.getExtras().get("pdus");
		StringBuilder smsBuilder = new StringBuilder(
				messages.length * SmsMessage.MAX_USER_DATA_BYTES);

		for (int n = 0; n < messages.length; n++)
		{
			String sms = SmsMessage.createFromPdu((byte[])messages[n]).getMessageBody();
			
			// check that the first chunk starts with our prefix
			if (n == 0 && !sms.startsWith(WAP_PREFIX)) {
				Log.d(LOG_TAG, "Non WAP_PREFIX SMS received (" + sms + ")");
				return;
			}
			
			if (messages.length > 1) {
				if (n == 0)
					Log.d(LOG_TAG, "Multipart SMS received");
				Log.d(LOG_TAG, "SMS part " + n + ": " + sms);
			}
			
			if (n == 0) {
				// on the first chunk, remove our prefix
				smsBuilder.append(sms.substring(WAP_PREFIX.length()));
			}
			else
				smsBuilder.append(sms);
		}
		
		nia = smsBuilder.toString();
		
		Log.i(LOG_TAG, "NIA message received: " + nia + "("+ nia.length() +")");
		sendEvent(context, ClientService.class, new Event("DMA_MSG_NET_NIA")
		.addVar(new EventVar("DMA_VAR_NIA_MSG", nia.getBytes()))
		.addVar(new EventVar("DMA_VAR_NIA_ENCODED", 0))
		);
	}
}
