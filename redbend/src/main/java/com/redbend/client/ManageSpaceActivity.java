/*
 *******************************************************************************
 *
 * ManageSpaceActivity.java
 *
 * Override Android's application clean data mechanism. Invoked when the
 * end-user taps Manage Space in Android's application settings menu.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.SmmService;
import com.redbend.client.BasicService;

public class ManageSpaceActivity extends Activity {
	
	private static final String LOG_TAG = "ManageSpaceActivity";

	public ManageSpaceActivity() {
	}
	
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		 Log.d(LOG_TAG, "+onCreate");
		 super.onCreate(savedInstanceState);
		 Intent i = new Intent(this, ClientService.class);
		 try {
			 /* this event is processed by intent service, and dropped, so it is not relevant for BL */
			 byte[] event = new Event(BasicService.CLEAR_DATA_EVENT).toByteArray();
			 i.putExtra(SmmService.flowIdExtra, 0);
			 i.putExtra(SmmService.startServiceMsgExtra, event);
			 startService(i);
		 } catch (IOException e) {
			Log.e(LOG_TAG, e.toString());
		 }
		 finish();
		 Log.d(LOG_TAG, "-onCreate");
	 }// onCreate

}
