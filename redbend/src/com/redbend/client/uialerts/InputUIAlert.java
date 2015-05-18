/*
 *******************************************************************************
 *
 * InputUIAlert.java
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
import android.widget.EditText;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;
import com.redbend.app.EventVar;

/**
 * Display text entry message screen. The screen displays an input field
 * prefilled with the text value DMA_VAR_UI_ALERT_TEXT and closes when the
 * end-user clicks OK or after DMA_VAR_UI_ALERT_MAXDT seconds have passed.
 *
 * Used during self-registration.
 */
public class InputUIAlert extends DilActivity {

		private final static int MILISECS_IN_SEC = 1000;
		private Button confirmButton;
		private Button cancelButton;

		private TextView uiAlertText;
		private EditText uiAlertTextInput;
		private boolean eventSent = false;
		private String text;
		private int    maxDT = 0;
		private CountDownTimer countDownTimer = null;
		
		@Override
		protected void setActiveView(boolean start, Event ev)
		{
			setContentView(R.layout.input_ui_alert);
			confirmButton = (Button)findViewById(R.id.ConfirmButton);
			cancelButton = (Button)findViewById(R.id.CancelButton);
			
			if (start)
			{
				byte[] t = ev.getVarStrValue("DMA_VAR_UI_ALERT_TEXT");
				if (t != null)
					text = new String(t);
				maxDT = ev.getVarValue("DMA_VAR_UI_ALERT_MAXDT");
				Log.d(LOG_TAG, "user_input_ui_alert, maxDT="+maxDT);
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
							sendEvent(new Event("DMA_MSG_TIMEOUT"));
							finish();
						}
					}.start();
				}

			}	

			uiAlertText = (TextView)findViewById(R.id.InputUIAlertText);
			if (text != null)
				uiAlertText.setText(new String(text));

			confirmButton.setOnClickListener(new OnClickListener() 
			{
				public void onClick(View v) 
				{
					uiAlertTextInput = (EditText)findViewById(R.id.InputUIAlertTextInput);
					Event event = new Event("DMA_MSG_USER_ACCEPT");
					event.addVar(new EventVar("DMA_VAR_UI_ALERT_TEXT", uiAlertTextInput.getText().toString()));
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

			cancelButton.setOnClickListener(new OnClickListener() 
			{
				public void onClick(View v) 
				{
					uiAlertTextInput = (EditText)findViewById(R.id.InputUIAlertTextInput);
					Event event = new Event("DMA_MSG_USER_CANCEL");
					Log.d(LOG_TAG, "CanceleButton clicked, event sent " + event.getName());
					
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
