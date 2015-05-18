/*
 *******************************************************************************
 *
 * ScomoInstallConfirm.java
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
import android.os.CountDownTimer;

import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Prompt the end-user to confirm or reject the installation.
 * If a critical update - wait's for 5 minutes for the user to select,
 * before proceeding with the update.
 */
public class ScomoInstallConfirm extends ScomoConfirmProgressBase {
	
	private CountDownTimer countDownTimer = null;
	private final static int MILISECS_IN_A_SEC   = 1000;
	private final static int DEFAULT_CONFIRM_TIMER_SEC   = 300;
	private final static int TIMER_INTERVAL_SEC  = 1;
	
	private final static String DMA_VAR_INS_CONFIRM_TIMER_SECONDS = "DMA_VAR_INS_CONFIRM_TIMER_SECONDS";
	private final static String DMA_MSG_SCOMO_INS_CONFIRM_UI = "DMA_MSG_SCOMO_INS_CONFIRM_UI";
	private final static String DMA_VAR_SCOMO_IS_NEED_REBOOT = "DMA_VAR_SCOMO_IS_NEED_REBOOT";
	private int m_isNeedReboot;
	private boolean m_isInstall = true;
	
	private void setSoftwareUpdateList(Event ev) {
		TextView textView = ((TextView)findViewById(R.id.InstallConfirmText));
		String softwareList = getAppListString(this, ev, true);
		if( softwareList.length() > 0) {
			m_isInstall = true;
			textView.setText(R.string.update_software_components);			
		} else {			
			softwareList = getAppListString(this, ev, false);
			if(softwareList.length() > 0){
				m_isInstall = false;
				textView.setText(R.string.remove_software_components);
			}
		}	
		textView = ((TextView)findViewById(R.id.InstallConfirmList));	
		textView.setText(softwareList);
	}
	
	private TextView m_timerText;
	private boolean m_eventSent = false;

	@Override
	protected void setActiveView(boolean start, Event ev) {
		Log.d("Activity", "ScomoInstallConfirm.setActiveView: " + start);
		
		setContentView(R.layout.scomo_install_confirm);	
		String eventName = ev.getName();

		if(!eventName.equals(DMA_MSG_SCOMO_INS_CONFIRM_UI)) {
			Log.i(LOG_TAG, "ScomoInstallConfirm activity got event, " + eventName + ", ignoring");
			return;
		}

		Button btnPostpone = ((Button)findViewById(R.id.InstallConfirmPostponeButton));	
		int isPostponeEnabled = ev.getVarValue(DMA_VAR_IS_POSTPONE_ENABLED);
		if(isPostponeEnabled == 1)
			btnPostpone.setVisibility(View.VISIBLE);
		else
			btnPostpone.setVisibility(View.GONE);
		
		m_isNeedReboot = ev.getVarValue(DMA_VAR_SCOMO_IS_NEED_REBOOT);
		Button btnCancel = ((Button)findViewById(R.id.InstallConfirmCancelButton));	
		int isCritical = ev.getVarValue(DMA_VAR_SCOMO_CRITICAL);		
		if(isCritical == 1) {
			
			Button btnOk = ((Button)findViewById(R.id.InstallConfirmConfirmButton));		
			btnOk.setText(getString(R.string.ok));
			
			btnCancel.setVisibility(View.GONE);
			if(start)
			{
				int intConfirmTimerSeconds = ev.getVarValue(DMA_VAR_INS_CONFIRM_TIMER_SECONDS);
				Log.d(LOG_TAG, "ScomoInstallConfirm timeout is " + intConfirmTimerSeconds + " seconds");
				if (intConfirmTimerSeconds == 0) {  //no such variable in the event
					Log.d(LOG_TAG, "ScomoInstallConfirm using the default timeout, which is " + intConfirmTimerSeconds + " seconds");
					intConfirmTimerSeconds = DEFAULT_CONFIRM_TIMER_SEC;
				}
				
				m_timerText = (TextView)findViewById(R.id.InstallConfirmTimerText);				
				countDownTimer = new CountDownTimer(intConfirmTimerSeconds * MILISECS_IN_A_SEC, TIMER_INTERVAL_SEC * MILISECS_IN_A_SEC) 
				{
					@Override 
					public void onTick(long millisUntilFinished) 
					{
						int resIdRemoveInstall = m_isInstall ? R.string.scomo_ins_confirm_timer
								: R.string.scomo_unins_confirm_timer;
						int resId = (m_isNeedReboot == 0) ? resIdRemoveInstall
								: R.string.scomo_ins_confirm_timer_reboot;
						
						m_timerText.setText(String.format(getString(resId),
								millisUntilFinished / 1000));
						m_timerText.invalidate();
					}
					
					@Override
					// make sure the timer or the button clicked causes an event, and not both
					public synchronized void onFinish() 
					{	         
						Button confirmButton = (Button)findViewById(R.id.InstallConfirmConfirmButton);
						confirmButton.setClickable(false);
						m_eventSent = true;
						sendEvent(new Event(DMA_MSG_SCOMO_ACCEPT));
					}
				}.start();	
			}
		}
		else 
			btnCancel.setVisibility(View.VISIBLE);

		// Set components details text
		setSoftwareUpdateList(ev);
		
		// Set footer string		
		if (m_isNeedReboot != 0) {
			TextView installConfirmFotter = ((TextView) findViewById(R.id.InstallConfirmFotter));
			String msgFotter = (isCritical == 1) ? getString(R.string.scomo_ins_confirm_critical_fotter)
					: getString(R.string.scomo_ins_confirm_fotter);
			installConfirmFotter.setText(msgFotter);
		}
		
		// Add release notes link AKA info URL
		createReleaseNotes(ev, R.id.scomoInstInfoUrl);
	}

	public void onButtonClicked(View v){
		if (countDownTimer != null)
			countDownTimer.cancel();
		
		if(v.getId() == R.id.InstallConfirmConfirmButton && !m_eventSent)
		{
			sendEvent(new Event(DMA_MSG_SCOMO_ACCEPT));
		}
		else if(v.getId() == R.id.InstallConfirmPostponeButton && !m_eventSent)
		{
			sendEvent(new Event(DMA_MSG_SCOMO_POSTPONE));
		}
		else if(v.getId() == R.id.InstallConfirmCancelButton)
		{
			sendEvent(new Event(DMA_MSG_SCOMO_CANCEL));		
			finish();
		}
	}
}
