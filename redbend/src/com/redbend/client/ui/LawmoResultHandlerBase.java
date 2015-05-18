/*
 *******************************************************************************
 *
 * LawmoResultHandlerBase.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

 /**
  * Notify end-user about a lock operation (base class).
  */
public class LawmoResultHandlerBase extends DilActivity {
	protected int showResourceId = 0;
	
	public LawmoResultHandlerBase(int resourceId){
		showResourceId = resourceId;
	}
	
	@Override
	protected void onStop() {
 		stopActivity();
		//This is last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}
	
	@Override
	protected void setActiveView(boolean start, Event receivedEvent) {
		String showText = getString(showResourceId);
		setContentView(R.layout.lawmo_operation_result);
				
		Button closeButton = (Button)findViewById(R.id.CloseButton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		TextView _textView = (TextView)findViewById(R.id.TextToShow);
		_textView.setText(showText);
		_textView.invalidate();
	}
}
