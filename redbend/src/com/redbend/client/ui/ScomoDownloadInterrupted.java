/*
 *******************************************************************************
 *
 * ScomoDownloadInterrupted.java
 *
 * Notifies the end-user that the download was interrupted.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_CANCELED_UI (foreground only)
 * DMA_MSG_DNLD_FAILURE (foreground only; on first non-silent retry only)
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

import com.redbend.android.RbException.VdmError;
import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.client.R;

/**
 * Notify that the download was interrupted.
 */
public class ScomoDownloadInterrupted extends ScomoConfirmProgressBase {	
	private TextView m_textViewHeader;
	private TextView m_textViewList;
	private TextView m_textViewFooter;
	private Button m_confirmButton;
	
	private final static String DMA_MSG_SCOMO_DL_CANCELED_UI = "DMA_MSG_SCOMO_DL_CANCELED_UI";
	private final static String DMA_MSG_DNLD_FAILURE = "DMA_MSG_DNLD_FAILURE";
	private final static String DMA_VAR_NETWORK_UI_REASONS = "DMA_VAR_NETWORK_UI_REASONS";
	private final static String DMA_VAR_ERROR = "DMA_VAR_ERROR";
	

 private static enum NetworkUIReason {
	E_FAILURE_UI_NO_ERROR,
	E_FAILURE_UI_UNKNOWN,
	E_FAILURE_UI_ROAMING,
	E_FAILURE_UI_NO_NETWORK,
	E_FAILURE_UI_WIFI_ONLY_WIFI_OFF
 }
 
 	
 	@Override
	protected void onStop() {
 		stopActivity();
		//This is an error activity - meaning its the last activity in the flow
		//if user decided to press home/back it will be finished
		finish();
	}

	@Override
	protected void setActiveView(boolean start, Event ev)
	{
		Log.d(LOG_TAG, "+setActiveView");
		
		setContentView(R.layout.scomo_download_interrupted);
		
		setInterruptionText(ev);			
		m_confirmButton = (Button)findViewById(R.id.ConfirmButton);
		
		m_confirmButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{		
				finish();
				Log.i(LOG_TAG, "close button was clicked, finish");
			}
		});
		
		Log.d(LOG_TAG, "-setActiveView");
	}
	
	private void setUpdatesListIntoTextViews(String inUpdateList) {
		m_textViewHeader.setText(getString(R.string.dl_interruption_activity_header) + " " + getString(R.string.dl_interruption_activity_footer));
		m_textViewList.setText(inUpdateList);
	}
	
	private void setInterruptionText(Event event) {
		String downloadList = getAppListString(this, event, true);
		if(downloadList.length() == 0)
			downloadList = getAppListString(this, event, false);
		
		if (m_textViewHeader == null)
			m_textViewHeader = (TextView) findViewById(R.id.DownloadInterruptedHeader);
		if (m_textViewList == null)
			m_textViewList = (TextView) findViewById(R.id.DownloadInterruptedList);
		if (m_textViewFooter == null)
			m_textViewFooter = (TextView) findViewById(R.id.DownloadInterruptedFooter);

		if (event.getName().equals(DMA_MSG_DNLD_FAILURE)) {
			EventVar network_reason_var = null;
			EventVar error_var = null;
			try {
				network_reason_var = event.getVar(DMA_VAR_NETWORK_UI_REASONS);
				error_var = event.getVar(DMA_VAR_ERROR);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				network_reason_var = null;
			}
			if (network_reason_var != null) {
				int network_interrupt_reason_val = network_reason_var
						.getValue();

				Log.d(LOG_TAG, "setInterruptionText=>network_interrupt_reason = "
						+ network_interrupt_reason_val);
				if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_WIFI_ONLY_WIFI_OFF
						.ordinal()) {
					m_textViewHeader.setText(getString(R.string.wifi_only));
				} else if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_ROAMING
						.ordinal()) {
					m_textViewHeader.setText(getString(R.string.roaming_zone));
				} else if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_NO_NETWORK
						.ordinal()) {
					m_textViewHeader.setText(getString(R.string.no_network));					
				} else if (error_var != null
						&& error_var.getValue() == VdmError.DL_OBJ_TOO_LARGE.val) {
					m_textViewHeader.setText(getString(R.string.no_disk_space));
				}
				else if (error_var != null
						&& error_var.getValue() == VdmError.COMMS_HTTP_FORBIDDEN.val) {
					m_textViewHeader.setText(getString(R.string.url_expiration));
				} 
				else if (error_var != null && 
						((error_var.getValue() == VdmError.BAD_DD_INVALID_SIZE.val) 
							|| (error_var.getValue() == VdmError.PURGE_UPDATE.val))) {
					m_textViewHeader.setText(getString(R.string.cancel_due_purge));
				} else {
					setUpdatesListIntoTextViews(downloadList);
				}
			}
		} else if (event.getName().equals(DMA_MSG_SCOMO_DL_CANCELED_UI)) {
			m_textViewHeader.setText(getString(R.string.download_canceled));
		} else {
			setUpdatesListIntoTextViews(downloadList);
		}
		Log.d(LOG_TAG, "-setInterruptionText");
	}
}
