/*
 *******************************************************************************
 *
 * LawmoLockResult.java
 *
 * Base class for:
 * - LawmoLockResult
 * - LawmoUnLockResult
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
  * Notify end-user about a lock operation (base class).
  */
public class LawmoLockResult extends DilActivity {
	
	@Override
	protected void onStop() {
 		stopActivity();
		//This is last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}
	
	@Override
	protected void setActiveView(boolean start, Event receivedEvent) {
		int showResourceId = 0;
		if("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS".equals(receivedEvent.getName())) {
			showResourceId = R.string.lock_finished;
		}else if ("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE".equals(receivedEvent.getName())){
			showResourceId = R.string.lock_finished_fail;
		}else if ("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS".equals(receivedEvent.getName())){
			showResourceId = R.string.unlock_finished;
		}else if ("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE".equals(receivedEvent.getName())){
			showResourceId = R.string.unlock_finished_fail;
		} else {
			finish();
			Log.e(LOG_TAG,"unknown event, finishing:"+receivedEvent.getName());
		}
		
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
