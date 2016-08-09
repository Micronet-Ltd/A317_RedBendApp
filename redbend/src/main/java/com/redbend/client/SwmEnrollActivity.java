/*
 *******************************************************************************
 *
 * SwmEnrollActivity.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.IOException;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmService;
import com.redbend.client.ClientService;

public class SwmEnrollActivity extends SwmStartupActivityBase {
	
	private final static String PACKAGE_NAME = "com.redbend.client";	
	
	private final static String START_ENROLLMENT_EVENT_NAME = "DMA_MSG_START_ENROLLMENT";	
		
	@Override 
	protected void sendStartServiceEvent(){
            //If has permission start enrollment
		Log.e(LOG_TAG, "+sendStartServiceEvent");
		Uri data = getIntent().getData();
		if (data == null){
			Log.e(LOG_TAG, "-sendStartServiceEvent:: URI is NULLL");
			return;
		}		
		Log.e(LOG_TAG, "uri = " + data.toString());
		
		Set<String> params = data.getQueryParameterNames();
		int size = params.size();
		Log.e(LOG_TAG, "params size = " + size);		
		
		Intent enrollIntent = new Intent(this, ClientService.class);	
		try {		
			Event event = new Event(START_ENROLLMENT_EVENT_NAME);
			for (int i = 0; i < size; i++) {
				String key = (String) params.toArray()[i];
				String value = data.getQueryParameter(key);

				Log.i(LOG_TAG, "key = " + key);
				Log.i(LOG_TAG, "value = " + value);

				event.addVar(new EventVar(key, value));
			}
			
			//add phone number to the intent
			TelephonyManager manager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			String phoneNumber = manager.getLine1Number();
			Log.d(LOG_TAG, "phoneNumber = " + phoneNumber);
			if (phoneNumber == null)
				phoneNumber = "";
			event.addVar(new EventVar("phoneNumber", phoneNumber));
						
			enrollIntent.putExtra(SmmService.flowIdExtra, 1);			
			enrollIntent.putExtra(SmmService.startServiceMsgExtra, event.toByteArray());

			enrollIntent.setPackage(PACKAGE_NAME);
			enrollIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);

			Log.i(LOG_TAG, "sending enrollment intent: " + enrollIntent);
			startService(enrollIntent);
		} catch (IOException e) {
			Log.e(LOG_TAG, "sendBroadcastEnrollmentEvent=>IOException " + e.toString());
		}
		Log.e(LOG_TAG, "-sendStartServiceEvent");
	}
	
	@Override
	protected void userAcceptedPermission(){
		sendStartServiceEvent();
	}
	
	@Override 
	protected void userDeclinedPermission(){
		//Do nothing in case of user declined
	}
}
