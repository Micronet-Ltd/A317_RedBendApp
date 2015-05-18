/*
 *******************************************************************************
 *
 * StartupActivity.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.IOException;

import com.redbend.app.Event;
import com.redbend.app.SmmService;

import android.content.Intent;
import android.util.Log;

/**
 * Start the application when the end-user clicks the application icon.
 */
public class StartupActivity extends SwmStartupActivityBase 
{
	private final static String USER_INITIATED_EVENT_NAME = "DMA_MSG_SESS_INITIATOR_USER_SCOMO";
	
	@Override 	
	protected void sendStartServiceEvent(){
		Intent userInitIntent = new Intent(this, ClientService.class);

		try {
			byte[] eventBuffer = new Event(USER_INITIATED_EVENT_NAME).toByteArray();

			userInitIntent.putExtra(SmmService.flowIdExtra, 1);
			userInitIntent.putExtra(SmmService.startServiceMsgExtra, eventBuffer);
			startService(userInitIntent);
		} catch (IOException e) {
			Log.e(LOG_TAG, "sendStartServiceEvent=>IOException: " + e.toString());
		}
	}
	
	@Override 
	protected void userAcceptedPermission(){
		sendStartServiceEvent();
	}
	
	@Override 
	protected void userDeclinedPermission(){
		sendStartServiceEvent();
	}
}
