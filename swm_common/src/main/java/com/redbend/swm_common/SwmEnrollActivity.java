/*
 *******************************************************************************
 *
 * SwmEnrollActivity.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import android.app.Activity;
import com.redbend.app.EventIntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.telephony.TelephonyManager;

import java.util.Set;

import android.net.Uri;

public abstract class SwmEnrollActivity extends Activity {

	protected final String LOG_TAG = getClass().getSimpleName() + 
									" (" + Integer.toHexString(hashCode()) + ")";
	
	public abstract String getIntentServicePackageName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.enroll_activity);
		
		Uri data = getIntent().getData();
		if (data==null)
			return;
		
		Log.d(LOG_TAG, "uri = " + data.toString());
		
		Set<String> params = data.getQueryParameterNames();
		int size = params.size();
		Log.e(LOG_TAG, "size = " + size);
		
		Intent enrollIntent = new Intent("com.redbend.event.DMA_MSG_START_ENROLLMENT");
		
		for (int i=0; i<size; i++){
			String key = (String) params.toArray()[i]; 
			String value = data.getQueryParameter(key);
			
			Log.i(LOG_TAG, "key = " + key);
			Log.i(LOG_TAG, "value = " + value);
			
			enrollIntent.putExtra(key, value);
		}
		
		//add phone number to the intent
		TelephonyManager manager = 
			(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = manager.getLine1Number();
		Log.d(LOG_TAG, "phoneNumber = " + phoneNumber);
		if (phoneNumber == null)
			phoneNumber = "";
		
		enrollIntent.putExtra("phoneNumber", phoneNumber);
		 
		enrollIntent.setPackage(getIntentServicePackageName());
		enrollIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		Log.i(LOG_TAG, "sending " + enrollIntent);
		sendBroadcast(enrollIntent, EventIntentService.PERMISSION);
		finish();
	}
}
