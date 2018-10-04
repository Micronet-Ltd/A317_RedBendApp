/*
 *******************************************************************************
 *
 * StartupActivityAutomotive.java
 *
 * Starts the application when the end-user taps the application icon. If Device
 * Administrator permission is disabled displays the Device Administrator dialog
 * box.
 * 
 * Sends BL events:
 * DMA_MSG_SESS_INITIATOR_USER_SCOMO
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
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
public class StartupActivityAutomotive extends SwmStartupActivityBase 
{
	private final static String D2B_CLIENT_STARTED_EVENT_NAME = "D2B_CLIENT_STARTED";
	
	@Override 	
	protected void sendStartServiceEvent(){
		Intent userInitIntent = new Intent(this, ClientService.class);

		try {
			byte[] eventBuffer = new Event(D2B_CLIENT_STARTED_EVENT_NAME).toByteArray();
			
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
