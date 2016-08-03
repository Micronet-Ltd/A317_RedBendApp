/*
 *******************************************************************************
 *
 * ScomoDownloadSuspend.java
 *
 * Notifies the end-user that the download was suspended. The screen displays
 * for DMA_VAR_SCOMO_DOWNLOAD_TIME_SECONDS seconds.
 * 
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_SUSPEND_UI (not silent update only, foreground only)
 * DMA_MSG_SCOMO_DL_SUSPEND_UI_FROM_ICON (not silent update only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.util.Calendar;
import java.util.Formatter;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Prompt the end-user to download suspended.
 */
public class ScomoDownloadSuspend extends DilActivity 
{
	
	@Override
	protected void setActiveView(boolean start, Event ev)
	{  	
		Log.i(LOG_TAG, "start="+start);
		setContentView(R.layout.scomo_download_suspend);
		TextView suspendText = (TextView)findViewById(R.id.scomoDlSuspendTextView);
		try {
			
			int dlTime = ev.getVar("DMA_VAR_SCOMO_DOWNLOAD_TIME_SECONDS").getValue();
			
			
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis((long)dlTime * 1000);
			String downloadTime = DateFormat.format(getString(R.string.date_format), calendar).toString();

			Formatter f = new Formatter();
			suspendText.setText(f.format(getString(R.string.scomo_dl_suspend), downloadTime).toString());
			f.close();
			
		} catch (Exception e) {
			suspendText.setText(getString(R.string.scomo_dl_suspend, "no time"));
		}
	}
	
	public void onButtonClicked(View v)
	{	
		if(v.getId() == R.id.ScomoConfirmButton)
		{
			finish();
		}
	}
}
