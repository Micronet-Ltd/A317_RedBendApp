/*
 *******************************************************************************
 *
 * InfoUIAlert.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
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
 * Display information message screen. The screen displays the message
 * DMA_VAR_UI_ALERT_TEXT and closes when the end-user clicks OK or after
 * DMA_VAR_UI_ALERT_MAXDT seconds have passed.
 */
public class InfoUIAlert extends DilActivity 
{
	private final static int MILISECS_IN_SEC = 1000;
	private Button confirmButton;

	private TextView uiAlertText;
	private boolean eventSent = false;
	private String text;
	private int    maxDT = 0;
	private CountDownTimer countDownTimer = null;
	
	@Override
	protected void setActiveView(boolean start, Event ev)
	{
		setContentView(R.layout.info_ui_alert);
		confirmButton = (Button)findViewById(R.id.ConfirmButton);

		if (start)
		{
			byte[] t = ev.getVarStrValue("DMA_VAR_UI_ALERT_TEXT");
			if (t != null)
				text = new String(t);
			maxDT = ev.getVarValue("DMA_VAR_UI_ALERT_MAXDT");
			Log.d(LOG_TAG, "information_ui_alert, maxDT="+maxDT);
			if (maxDT > 0)
			{
				countDownTimer = new CountDownTimer(maxDT*MILISECS_IN_SEC, MILISECS_IN_SEC)
				{
					@Override
					public void onTick(long millisUntilFinished) {
						// nothing to do on tick
					}
					
					@Override
					public synchronized void onFinish() 
					{	         					
						Log.d(LOG_TAG, "countDownTimer finished");
						eventSent = true;
						sendEvent(new Event("DMA_MSG_TIMEOUT"));
						finish();
					}
				}.start();
			}

		}	

		uiAlertText = (TextView)findViewById(R.id.InfoUIAlertText);
		if (text != null)
			uiAlertText.setText(new String(text));

		confirmButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Event event = new Event("DMA_MSG_USER_ACCEPT"); 
				Log.d(LOG_TAG, "ConfirmButton clicked, event sent " + event.getName());
				
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
		});	
	}
}
