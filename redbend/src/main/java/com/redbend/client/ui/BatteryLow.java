/*
 *******************************************************************************
 *
 * BatteryLow.java
 *
 * Notifies the end-user that the update was interrupted by a low battery. After
 * user-confirmation, the business logic checks the battery level using
 * GetBatteryLevelHandler. If the battery level is too low, this class handles
 * foreground notification while the class BatteryLowNotificationHandler
 * handles background notification. The notification displays the required
 * battery threshold (DMA_VAR_BATTERY_THRESHOLD).
 * 
 * Receives DIL events:
 * DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI (foreground only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Check if the battery level is insufficient to proceed with the update.
 */
public class BatteryLow extends DilActivity 
{
	private Button confirmButton;
	
	@Override
	protected void setActiveView(boolean start, Event ev)
	{  	
		Log.i(LOG_TAG, "start="+start);
		setContentView(R.layout.charger_request);
		TextView chargeText = (TextView)findViewById(R.id.charge_request_text_id);
		try {
			
			int threshold = ev.getVar("DMA_VAR_BATTERY_THRESHOLD").getValue();
			chargeText.setText(getString(R.string.charge_request_text, threshold + "%"));
			
		} catch (Exception e) {
			chargeText.setText(getString(R.string.charge_request_text, "threshold"));
		}
		
		confirmButton = (Button)findViewById(R.id.battery_not_enough_OK);
		confirmButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) {
				finish();
			}
		});	
	}
	
 	@Override
	protected void onStop() {
 		stopActivity();
		//This is an error activity - meaning its the last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}
}
