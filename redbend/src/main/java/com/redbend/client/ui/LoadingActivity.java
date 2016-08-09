/*
 *******************************************************************************
 *
 * LoadingActivity.java
 *
 * Notifies the end-user that the device is checking for new updates.
 *
 * Receives DIL events:
 * DMA_MSG_USER_SESSION_TRIGGERED (foreground only)
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
 * Notify end-user that the installation is complete.
 */
public class LoadingActivity extends DilActivity {
	private TextView m_textView;
	private Button m_confirmButton;
	private int LOADING_PAGE_CHECKING = 0;

	@Override
	protected void setActiveView(boolean start, Event ev) {
		String resultText;

		setContentView(R.layout.loading_activity);
		m_textView = (TextView) findViewById(R.id.loading_text);
		m_confirmButton = (Button) findViewById(R.id.ConfirmButton);
		m_confirmButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG,
						"LoadingActivity: 'ok' button was clicked, finish activity");
				moveTaskToBack(true);
			}
		});
		int messageType = ev.getVarValue("DMA_VAR_LOADING_MSG_TYPE");
		if (messageType == LOADING_PAGE_CHECKING)
			resultText = getString(R.string.loading);
		else
			resultText = getString(R.string.scomo_dm_during_initialize);
		m_textView.setText(resultText);
	}
}
