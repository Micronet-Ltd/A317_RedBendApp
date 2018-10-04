/*
 *******************************************************************************
 *
 * ScomoInstallDone.java
 *
 * Notifies the end-user that the installation is complete.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_INSTALL_DONE (not silent update only, foreground only)
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
import com.redbend.swm_common.SmmCommonConstants;

/**
 * Notify end-user that the installation is complete.
 */
public class ScomoInstallDone extends DilActivity
{	
	private TextView textView;
	private Button confirmButton;
	private int FUMO_SUCCESS = 200;
	private int SCOMO_SUCCESS = 1200;
	private int SCOMO_PARTIAL_SUCCESS = 1452;

	@Override
	protected void onStop() {
 		stopActivity();
		//This is last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}

	@Override
	protected void setActiveView(boolean start, Event ev)
	{
		int result = 0;
		int operationType = SmmCommonConstants.E_DP_Type_Fumo;
		String resultText;
		
		try {
			result = ev.getVar("DMA_VAR_REPORT_RESULT").getValue();
			operationType = ev.getVar("DMA_VAR_OPERATION_TYPE").getValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setContentView(R.layout.scomo_install_done);
		textView = (TextView) findViewById(R.id.ScomoInstallDoneText);
		confirmButton = (Button)findViewById(R.id.ConfirmButton);
		confirmButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{		
				Log.i(LOG_TAG, "ScomoInstallDone: 'ok' button was clicked, finish activity");
				finish();
			}
		});

		if (operationType == SmmCommonConstants.E_DP_Type_Fumo ||
			operationType == SmmCommonConstants.E_DP_Type_FumoInScomo)
		{
			if (result == FUMO_SUCCESS)
				resultText = getString(R.string.fumo_update_done);
			else
				resultText = getString(R.string.fumo_update_fail);
		}
		else {
			if (result == SCOMO_SUCCESS)
				resultText = getString(R.string.scomo_update_done);
			else if (result == SCOMO_PARTIAL_SUCCESS)
				resultText = getString(R.string.update_partial_success);
			else
				resultText = getString(R.string.scomo_update_fail);
		}

		textView.setText(resultText);
	}
}
