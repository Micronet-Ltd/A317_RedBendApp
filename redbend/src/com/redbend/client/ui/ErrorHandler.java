/*
 *******************************************************************************
 *
 * ErrorHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

import android.util.Log;
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
	public final static int ERR_TYPE_USER_INTERACTION_TIMEOUT = 0x6389;
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
		//This is an error activity - meaning its the last activity in the flow
		//if user decided to press home/back it will be finished
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
		case ERR_TYPE_DL_GENERAL:
			errorStr = getString(R.string.general_dl_error);
			break;
		default:
			errorStr = isDlSessionType?  getString(R.string.general_dl_error) :  
						getString(R.string.general_dm_error);
			break;
		}
		
		return errorStr;
	}
}
