/*
 *******************************************************************************
 *
 * ErrorHandler.java
 *
 * Error handling, displays an error message.
 *
 * Receives DIL events:
 * DMA_MSG_DM_ERROR_UI
 * DMA_MSG_DL_ERROR_UI
 * DMA_MSG_DL_INST_ERROR_UI
 * DMA_MSG_SCOMO_DL_CONFIRM_UI (user initiated and silent campaign only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;
import com.redbend.client.RbAnalyticsHelper;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Error handling: display m_error message.
 *
 * \note	English m_error messages are embedded in this class.
 */
public class ErrorHandler extends DilActivity 
{
	public final static int ERR_TYPE_NONE = 0;
	/* Adding values in vendor specific range, so it won't contradicts with vDM m_error values */
	public final static int ERR_TYPE_DL_GENERAL 			 = 0x6380;
	public final static int ERR_TYPE_DL_NETWORK 			 = 0x6381;
	public final static int ERR_TYPE_DM_GENERAL 			 = 0x6382;
	public final static int ERR_TYPE_DM_NETWORK				 = 0x6383;
	public final static int ERR_TYPE_DM_NO_PKG 				 = 0x6384;
	public final static int ERR_TYPE_DM_SESSION_IN_PROGRESS  = 0x6385;
	public final static int ERR_TYPE_ROAMING_OR_EMERGENCY    = 0x6386;
	public final static int ERR_TYPE_FLOW_IN_PROGRESS		 = 0x6388;
	public final static int ERR_TYPE_USER_INTERACTION_TIMEOUT 	= 0x6389;
	public final static int ERR_TYPE_INSTALLATION_COND_TIMEOUT 	= 0x638A;
	public final static int ERR_TYPE_INSTALLATION_COND_XML_ERROR = 0x638B;
	public final static int ERR_TYPE_PURGE_UPDATE				 = 0x638C;
	public final static int ERR_TYPE_CONDITIONS_BEFORE_DM 		= 0x638E;	//!< Before DM conditions failed
	public final static int ERR_TYPE_INITIALIZE_IN_PROGRESS     = 0x638F;	//!< Initialize in Progress Error
	public final static int ERR_TYPE_DM_ACCOUNT_DOES_NOT_EXIST = 0x9003;
	private int m_error = ERR_TYPE_NONE;

	@Override
	protected void setActiveView(boolean start, Event receivedEvent)
	{
		setContentView(R.layout.show_error);
		if( receivedEvent == null)
			return;
			
		String eventName = receivedEvent.getName();		
		String errorText = null;		
		
		if(eventName.equals("DMA_MSG_SCOMO_DL_CONFIRM_UI")){
			m_error = ERR_TYPE_FLOW_IN_PROGRESS;
			errorText = mapErrorToString(false);
		} else {
			if (start) {
				m_error = receivedEvent.getVarValue("DMA_VAR_ERROR");
				Log.v(LOG_TAG, "Showing m_error 0x" + Integer.toHexString(m_error));
			}
			errorText = mapErrorToString(eventName.equals("DMA_MSG_DL_INST_ERROR_UI"));
		}
		
		TextView errTextView = (TextView)findViewById(R.id.ErrorText);
		errTextView.setText(errorText);
		
		Button confirmButton = (Button)findViewById(R.id.ConfirmButton);	
		confirmButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{				
				Log.d(LOG_TAG, " ConfirmButton clicked");
				finish();
			}
		});		
	}
	
	@Override
	protected void onStop() {
		stopActivity();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		//This is an error activity - meaning its the last activity in the flow
		//if user decided to press back it will be finished
		finish();	
	}
	
	private String mapErrorToString(boolean isDlSessionType)
	{
		String errorStr = null; 
		
		switch(m_error)
		{
		case ERR_TYPE_DL_NETWORK:	
		case ERR_TYPE_DM_NETWORK:
			errorStr = getString(R.string.no_network);
			break;
		case ERR_TYPE_DM_NO_PKG:
			errorStr = getString(R.string.no_updates);
			break;
		case ERR_TYPE_DM_SESSION_IN_PROGRESS:
			errorStr = getString(R.string.another_session_in_progress);
			break;
		case ERR_TYPE_FLOW_IN_PROGRESS:
			errorStr = getString(R.string.flow_in_progress);
			break;						
		case ERR_TYPE_ROAMING_OR_EMERGENCY:
			errorStr = getString(R.string.roaming_zone);
			break;
		case ERR_TYPE_USER_INTERACTION_TIMEOUT:
			errorStr = getString(R.string.user_interaction_timeout);
			break;
		case ERR_TYPE_DM_GENERAL:
			errorStr = getString(R.string.general_dm_error);
			break;		
		case ERR_TYPE_DM_ACCOUNT_DOES_NOT_EXIST:
			errorStr = getString(R.string.acc_does_not_exists_dm_error);
			break;
		case ERR_TYPE_DL_GENERAL:
			errorStr = getString(R.string.general_dl_error);
			break;
		case ERR_TYPE_INSTALLATION_COND_TIMEOUT:
			errorStr = getString(R.string.installtion_cond_timeout);
			break;
		case ERR_TYPE_INSTALLATION_COND_XML_ERROR:
		case ERR_TYPE_CONDITIONS_BEFORE_DM:
			errorStr = getString(R.string.installtion_cond_error);
			break;
		case ERR_TYPE_INITIALIZE_IN_PROGRESS:
			errorStr = getString(R.string.scomo_dm_during_initialize);
			break;
		case ERR_TYPE_PURGE_UPDATE:
			errorStr = getString(R.string.cancel_due_purge);
			break;
		default:
			errorStr = isDlSessionType?  getString(R.string.general_dl_error) :  
						getString(R.string.general_dm_error);
			break;
		}
		
		return errorStr;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		if (RbAnalyticsHelper.isRbAnalyticsDelivery(getApplicationContext()))
		{
			super.onCreateOptionsMenu(menu);
		    MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.menu, menu);
		}
	    
	    return true;
	} 
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu)
	{
		if (RbAnalyticsHelper.isRbAnalyticsDelivery(getApplicationContext()))
		{
			super.onPrepareOptionsMenu(menu);
			boolean serviceRunning = RbAnalyticsHelper.isRbAnaliticsRunning(getApplicationContext());
			menu.findItem(R.id.allow_analytics).setChecked(serviceRunning);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.allow_analytics){
			if (item.isChecked()){
				item.setChecked(false);
				Log.d(LOG_TAG, "STOP ananlytics service");
			}
			else{
				item.setChecked(true);
				Log.d(LOG_TAG, "STARTING ananlytics service");
			}
			
			RbAnalyticsHelper.setRbAnalyticsServiceState(getApplicationContext(), item.isChecked());
			
			return true;
		}
		Log.d(LOG_TAG, "unknown menu item");
		return super.onOptionsItemSelected(item);
	}
}
