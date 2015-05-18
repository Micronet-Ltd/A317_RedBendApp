/*
 *******************************************************************************
 *
 * LawmoWipeResult.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
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
 * Notify on wipe data operation result.
 */
public class LawmoWipeResult extends DilActivity {

	@Override
	protected void onStop() {
 		stopActivity();
		//This is last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}
	
	@Override
	protected void setActiveView(boolean start, Event receivedEvent) {
		
		TextView textView;
		String eventName = receivedEvent.getName();
		
		Log.d(LOG_TAG, "LawmoWipeResult: setActiveView. receivedMsg=" + eventName);
		setContentView(R.layout.lawmo_operation_result);
				
		Button closeButton = (Button)findViewById(R.id.CloseButton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});	
				
		textView = (TextView)findViewById(R.id.TextToShow);
		
		if (eventName.equals("DMA_MSG_LAWMO_WIPE_RESULT_SUCCESS"))
				textView.setText(R.string.wipe_success);
		else if (eventName.equals("DMA_MSG_LAWMO_WIPE_RESULT_FAILURE"))
				textView.setText(R.string.wipe_failure);
		else if (eventName.equals("DMA_MSG_LAWMO_WIPE_RESULT_NOT_PERFORMED"))
				textView.setText(R.string.wipe_not_performed);
		
		textView.invalidate();
	}
}
