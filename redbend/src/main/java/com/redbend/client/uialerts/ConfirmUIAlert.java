/*
 *******************************************************************************
 *
 * ConfirmUIAlert.java
 *
 * Displays confirmation message screen. The screen displays the message
 * DMA_VAR_UI_ALERT_TEXT and closes when the end-user taps OK or Cancel or after
 * DMA_VAR_UI_ALERT_MAXDT seconds have passed. The preselected value, if any, is
 * defined by the value in DMA_VAR_UI_ALERT_DEFAULT_CMD. Used during
 * self-registration.
 *
 * Receives DIL events:
 * DMA_MSG_UI_ALERT if DMA_VAR_UI_ALERT_TYPE=DMA_UI_ALERT_TYPE_CONFIRMATION (foreground only)
 * 
 * Sends BL events:
 * DMA_MSG_USER_ACCEPT or DMA_MSG_USER_CANCEL: if the end-user closes the message
 * D2B_UI_ALERT_TIMEOUT: if the message times out
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;


/**
 * Display confirmation message screen.
 *
 * The screen displays the message DMA_VAR_UI_ALERT_TEXT and closes when the
 * end-user clicks OK or Cancel or after DMA_VAR_UI_ALERT_MAXDT seconds have
 * passed. The preselected value, if any, is defined by the value in
 * DMA_VAR_UI_ALERT_DEFAULT_CMD.
 * 
 * Used during self-registration.
 */
public class ConfirmUIAlert extends DilActivity 
{
	private final static int MILISECS_IN_SEC = 1000;
	private String text;
	private int    maxDT = 0;
	private int    defaultCmd = 0;
	private CountDownTimer countDownTimer = null;
	private boolean eventSent = false;
	
	@Override
	protected void setActiveView(boolean start, Event ev)
	{
		setContentView(R.layout.confirm_ui_alert);
		defaultCmd = ev.getVarValue("DMA_VAR_UI_ALERT_DEFAULT_CMD");
		if (start)
		{
			byte[] t = ev.getVarStrValue("DMA_VAR_UI_ALERT_TEXT");
			if (t != null)
				text = new String(t);			
		}
        
		if (start)
		{
			maxDT = ev.getVarValue("DMA_VAR_UI_ALERT_MAXDT");
			Log.d(LOG_TAG, "confirm_ui_alert, maxDT="+maxDT);
			if (maxDT > 0)
			{
				countDownTimer = new CountDownTimer(maxDT*MILISECS_IN_SEC, MILISECS_IN_SEC)
				{
					@Override
					public void onTick(long millisUntilFinished) {
						// nothing to do on every tick
					}
					
					@Override
					public synchronized void onFinish() {
						Log.d(LOG_TAG, "countDownTimer finished");					
						eventSent = true;
						sendEvent(new Event("D2B_UI_ALERT_TIMEOUT"));
						finish();
					}
				}.start();
			}
		}
		
		TextView confirmUIAlertText = (TextView)findViewById(R.id.ConfirmUIAlertText);	
		if (text != null)
			confirmUIAlertText.setText(text);

		Button confirmButton = (Button)findViewById(R.id.ConfirmButton);
		confirmButton.setOnClickListener(new ButtonClickListener(
				new Event("DMA_MSG_USER_ACCEPT"), "ConfirmButton"));

		//This button is for cancellation (returns 214) -
		//not for a 'no' answer (which returns 304)
		Button cancelButton = (Button)findViewById(R.id.CancelButton);
		cancelButton.setOnClickListener(new ButtonClickListener( 
				new Event("DMA_MSG_USER_CANCEL"), "CancelButton"));

		
		if (defaultCmd == 0)
		{
			cancelButton.requestFocus();
		}
		else
		{
			confirmButton.requestFocus();
		}
	}
	
	// class implementing the common functionality for all buttons in the screen
	private class ButtonClickListener implements OnClickListener
	{
		private Event event;
		private String btnName;

		public ButtonClickListener(Event event, String btnName)
		{
			this.event = event;
			this.btnName = btnName;
		}

		public void onClick(View v) 
		{			
			Log.d(LOG_TAG, btnName + " clicked sending event " + event.getName());
			
			if (countDownTimer != null)
			{
			   countDownTimer.cancel();
			}
			
			if (eventSent)
			{
				return;
			}
			sendEvent(event);
			finish();
		}
	}
}
