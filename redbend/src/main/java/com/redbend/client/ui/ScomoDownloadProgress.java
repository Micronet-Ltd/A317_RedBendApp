/*
 *******************************************************************************
 *
 * ScomoDownloadProgress.java
 *
 * Displays download progress. Displays a cancel button if not a critical
 * update.
 * 
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_PROGRESS (not silent update only)
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Display download progress. Display a cancel button if not a critical update.
 */
public class ScomoDownloadProgress extends ScomoConfirmProgressBase {
	
	protected final static String DMA_MSG_SCOMO_DL_PROGRESS = "DMA_MSG_SCOMO_DL_PROGRESS";
	protected final static String DMA_VAR_DL_PROGRESS = "DMA_VAR_DL_PROGRESS";
	
	private int m_progress = 0;
		
	@Override
	protected void setActiveView(boolean start, Event ev) {
		Log.d("Activity", "ScomoDownloadProgress.setActiveView: " + start);
		
		setContentView(R.layout.scomo_progress);
		
		TextView titleTextView = (TextView) findViewById(R.id.progressTitle);
		titleTextView.setText(getString(R.string.scomo_download_in_progress_3dot));
		
		String eventName = ev.getName();
		if (eventName.equals(DMA_MSG_SCOMO_DL_PROGRESS)) {
			int isCritical = ev.getVarValue(DMA_VAR_SCOMO_CRITICAL);
			Button cancelButton = (Button) findViewById(R.id.scomoDownloadCancelButton);
			if (isCritical == 0){
				cancelButton.setEnabled(true);
				cancelButton.setVisibility(View.VISIBLE);
				cancelButton.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						sendEvent(new Event(DMA_MSG_SCOMO_CANCEL));
					}
				});			
			}
			else {
				cancelButton.setVisibility(View.INVISIBLE);
				LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayout2);
				ll.setVisibility(View.INVISIBLE);
			}

			TextView sizeTextView = ((TextView) findViewById(R.id.downloadingSize));
			sizeTextView.setText(getDpSizeText(ev));
			
			String appsList = getAppListString(this, ev, true);

			TextView updatedListTextView = ((TextView) findViewById(R.id.downloadingText));	
			updatedListTextView.setText(appsList);
		}
		
		ProgressBar progressBar = (ProgressBar)findViewById(R.id.scomoDownloadProgressBar);
		progressBar.setMax(100);			
		updateProgress(ev);
	}
		
	private void updateProgress(Event ev) {
		int progress = ev.getVarValue(DMA_VAR_DL_PROGRESS);
		if (progress == 0)
			progress = m_progress;		
		
		if (progress != m_progress) 
			m_progress = progress;			

		Log.d("Activity", "ScomoDownloadProgress.updateProgress:progress = " + m_progress);
		
		ProgressBar progressBar = (ProgressBar)findViewById(R.id.scomoDownloadProgressBar);
		if (progressBar != null) {
			progressBar.setProgress(m_progress);
        }
	
		TextView downloadProgressTextView = ((TextView) findViewById(R.id.scomoDownloadProgressTextView));
		if (downloadProgressTextView != null) {
			downloadProgressTextView.setText(String.format(getString(R.string.scomo_percent), m_progress));
        }
	}
	
	@Override
	protected void newEvent(Event ev) {		
		String eventName = ev.getName();
		Log.d("Activity", "+ScomoDownloadProgress.newEvent");
		
		if (eventName.equals(DMA_MSG_SCOMO_DL_PROGRESS)) {
			m_progress = 0;
			updateProgress(ev);
		}
	}

}
