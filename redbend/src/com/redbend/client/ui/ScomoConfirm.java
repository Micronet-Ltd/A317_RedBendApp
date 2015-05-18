/*
 *******************************************************************************
 *
 * ScomoConfirm.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Bundle;

import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Prompt the end-user to confirm or reject the download.
 */
public class ScomoConfirm extends ScomoConfirmProgressBase {
	
	private final static String DMA_MSG_SCOMO_DL_CONFIRM_UI = "DMA_MSG_SCOMO_DL_CONFIRM_UI";	
	
//	private NotificationManager notifManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void setActiveView(boolean start, Event ev) {
		Log.d("Activity", "ScomoConfirm.setActiveView: " + start);
		setContentView(R.layout.scomo_confirm);	
		
		String eventName = ev.getName();

		if (!eventName.equals(DMA_MSG_SCOMO_DL_CONFIRM_UI))	{
			Log.i(LOG_TAG, "ScomoConfirm activity got event, " + eventName + ", ignoring");
			return;
		}
		
		Button btnPostpone = ((Button)findViewById(R.id.ScomoDlPostponeButton));	
		int isPostponeEnabled = ev.getVarValue(DMA_VAR_IS_POSTPONE_ENABLED);
		if(isPostponeEnabled == 1)
			btnPostpone.setVisibility(View.VISIBLE);
		else
			btnPostpone.setVisibility(View.GONE);
					
		String msg = "";
		
		TextView textView = ((TextView)findViewById(R.id.scomoConfirmTextView));		
		msg = String.format(getString(R.string.scomo_dl_confirm),
				getDpSizeText(ev));
		textView.setText(msg);		

		TextView updateOrRemoveTitle = ((TextView)findViewById(R.id.updateRemoveTitle));
		
		
		String compsList = getAppListString(this, ev, true);		
		if (compsList.length() > 0) {
			updateOrRemoveTitle.setText(getString(R.string.update_software_components_download));	
		} else{
			compsList = getAppListString(this, ev, false);
			if (compsList.length() > 0)
				updateOrRemoveTitle.setText(getString(R.string.remove_software_components_download));
		}		
		TextView compsListTextView = ((TextView) findViewById(R.id.updateRemoveList));
		compsListTextView.setText(compsList);

		// Add release notes link AKA info URL
		createReleaseNotes(ev, R.id.scomoDlInfoUrl);
	}
	
	public void onButtonClicked(View v)	{
		Event ev = null;
		if(v.getId() == R.id.ScomoDlConfirmButton)	{
			ev = new Event(DMA_MSG_SCOMO_ACCEPT);			
		}
		else if(v.getId() == R.id.ScomoDlPostponeButton){
			ev = new Event(DMA_MSG_SCOMO_POSTPONE);			
		}
		else if(v.getId() == R.id.ScomoDlCancelButton)	{
			ev = new Event(DMA_MSG_SCOMO_CANCEL);			
		}
		if(ev != null){
			sendEvent(ev);
		}
	}
}
