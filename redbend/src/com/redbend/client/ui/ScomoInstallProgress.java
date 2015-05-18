/*
 *******************************************************************************
 *
 * ScomoInstallProgress.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;


import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Display download progress. Display a cancel button if not a critical update.
 */
public class ScomoInstallProgress extends ScomoConfirmProgressBase {	
	
	protected final static String DMA_MSG_SCOMO_INSTALL_PROGRESS_UI = "DMA_MSG_SCOMO_INSTALL_PROGRESS_UI";
	protected final static String DMA_VAR_SCOMO_INSTALL_PROGRESS = "DMA_VAR_SCOMO_INSTALL_PROGRESS";
	
	
	private int m_progress = 0;
	ProgressBar m_progressBar  = null;
	TextView m_scomoInstallProgressTextView = null;

	TextView m_sizeTextView = null;
	TextView m_updatedListTextView = null;
	
	@Override
	protected void setActiveView(boolean start, Event ev) {
		Log.d("Activity", "ScomoInstallProgress.setActiveView: " + start);
		
		setContentView(R.layout.scomo_install_progress);
		
		TextView titleTextView = (TextView) findViewById(R.id.progressTitle);
		titleTextView.setText(getString(R.string.scomo_installation_in_progress_3dot));
		
		String eventName = ev.getName();
		if (eventName.equals(DMA_MSG_SCOMO_INSTALL_PROGRESS_UI)) {
				
			if(m_sizeTextView == null)
				m_sizeTextView = ((TextView) findViewById(R.id.dpSize));			
			m_sizeTextView.setText(getDpSizeText(ev));
			
			String appsList = getAppListString(this, ev, true);
			if(m_updatedListTextView == null)
				m_updatedListTextView = ((TextView) findViewById(R.id.insatllingText));

			m_updatedListTextView.setText(appsList);
		}
		
		if(m_progressBar == null){
			m_progressBar = (ProgressBar) findViewById(R.id.scomoInstallProgressBar);
			m_progressBar.setMax(100);	
		}
			
		updateProgress(ev);
	}
		
	private void updateProgress(Event ev) {
		int progress = ev.getVarValue(DMA_VAR_SCOMO_INSTALL_PROGRESS);
		if(progress == 0)
			progress = m_progress;		
		
		if(progress != m_progress) 
			m_progress = progress;			

		Log.d("Activity", "ScomoInstallProgress.updateProgress:progress = " + m_progress);
		
		if(m_progressBar == null)
			m_progressBar = (ProgressBar) findViewById(R.id.scomoInstallProgressBar);
		m_progressBar.setProgress(m_progress);
	
		if(m_scomoInstallProgressTextView == null)
			m_scomoInstallProgressTextView = ((TextView) findViewById(R.id.scomoInstallProgressTextView));
		m_scomoInstallProgressTextView.setText(String.format(getString(R.string.scomo_percent), m_progress));		
	}
	
	@Override
	protected void newEvent(Event ev) {		
		String eventName = ev.getName();
		Log.d("Activity", "+ScomoInstallProgress.newEvent");
		
		if (eventName.equals(DMA_MSG_SCOMO_INSTALL_PROGRESS_UI)) {
			m_progress = 0;
			updateProgress(ev);
		}
	}
}
