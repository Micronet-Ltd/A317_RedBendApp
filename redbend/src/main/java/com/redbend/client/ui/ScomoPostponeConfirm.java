/*
 *******************************************************************************
 *
 * ScomoPostponeConfirm.java
 *
 * Informs the end-user that a download (if DMA_VAR_DURING_DL is True) or
 * installation (if DMA_VAR_DURING_DL is False) will start in
 * DMA_VAR_SCOMO_POSTPONE_PERIOD minutes.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_POSTPONE_STATUS_UI
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.util.Formatter;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Prompt the end-user to confirm postpone.
 */
public class ScomoPostponeConfirm extends DilActivity {

	int m_period;
	private final static int MILISECS_IN_A_SEC   = 1000;
	private final static int TIMER_INTERVAL_SEC  = 30;

	@Override
	protected void setActiveView(boolean start, Event ev) {
		Log.d(LOG_TAG, "setActiveView: " + start);
		setContentView(R.layout.scomo_postpone_confirm);	

		String eventName = ev.getName();

		if (!eventName.equals("DMA_MSG_SCOMO_POSTPONE_STATUS_UI"))
		{
			Log.i(LOG_TAG, "Activity got event, " + eventName + ", ignoring");
			return;
		}

		try {
			m_period = ev.getVar("DMA_VAR_SCOMO_POSTPONE_PERIOD").getValue();
			final int duringDl = ev.getVar("DMA_VAR_DURING_DL").getValue();
			Log.d(LOG_TAG, "m_period: " + m_period + " duringDl:"+ duringDl);
			new CountDownTimer( m_period * MILISECS_IN_A_SEC, TIMER_INTERVAL_SEC * MILISECS_IN_A_SEC) 
			{
				@Override 
				public void onTick(long millisUntilFinished) 
				{
					Log.d(LOG_TAG, "millisUntilFinished: " + millisUntilFinished);
					Formatter format = new Formatter();
					String action = (duringDl==1) ? getString(R.string.download_confirm) : 
						getString(R.string.install_confirm);

					String postponeStr = format.format(getString(R.string.scomo_postpone_text),
							action,
							formatPeriod((int)(millisUntilFinished/1000+1))).toString();// adding a second as it takes time to get here
					Log.d(LOG_TAG,"postponeStr: " + postponeStr);
					format.close();
					TextView confirmText = ((TextView)findViewById(R.id.ScomoPostponeConfirmationText));
					confirmText = ((TextView)findViewById(R.id.ScomoPostponeConfirmationText));
					confirmText.setText(postponeStr);					
					confirmText.invalidate(); 
				}

				@Override
				// make sure the timer or the button clicked causes an event, and not both
				public synchronized void onFinish() 
				{	         
					// nothing to do here
				}
			}.start();
		} catch (Exception e) {
			Log.e(LOG_TAG, "setActiveView" + e.toString());
			return;
		}

	}

	private String formatPeriod(int period){
		Log.d(LOG_TAG, "formatPeriod:" + period);

		if(period < 1)
			return "";

		int hours = period / 3600;
		int min = (period / 60) % 60;

		String ret = "";

		if(hours == 1)
			ret = hours + " " + getString(R.string.hour);
		else if(hours > 1)
			ret = hours + " "  + getString(R.string.hours);

		if(min > 0){
			if(hours > 0) ret += " and ";
			String minSufix = (min == 1)?
				getString(R.string.minute) : getString(R.string.minutes);
			ret += min + " " + 	minSufix;
		}
		Log.d(LOG_TAG, "formatPeriod:" + ret);
		return ret;
	}

	public void onButtonClicked(View v)
	{	
		if(v.getId() == R.id.PostponeConfirmButton)
		{
			finish();
		}
	}

}
