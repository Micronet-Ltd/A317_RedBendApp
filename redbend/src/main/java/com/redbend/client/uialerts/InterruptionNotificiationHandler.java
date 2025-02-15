/*
 *******************************************************************************
 *
 * InterruptionNotificiationHandler.java
 *
 * Displays a notification that the download was interrupted.
 *
 * Receives DIL events:
 * DMA_MSG_DNLD_FAILURE (not silent update only, background only)
 * DMA_MSG_SCOMO_DL_CANCELED_UI (background only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import java.util.EmptyStackException;

import android.app.*;

import com.redbend.app.*;
import com.redbend.client.ClientService;
import com.redbend.client.R;

import android.content.Context;
import android.util.Log;
import com.redbend.android.RbException.VdmError;

/**
 * Display notification that the download was interrupted.
 */
public class InterruptionNotificiationHandler extends EventHandler {

	private final String LOG_TAG = "InterruptionNotificiationHandler::";
	
	private static enum NetworkUIReason {
			E_FAILURE_UI_NO_ERROR,
			E_FAILURE_UI_UNKNOWN,
			E_FAILURE_UI_ROAMING,
			E_FAILURE_UI_NO_NETWORK,
			E_FAILURE_UI_WIFI_ONLY_WIFI_OFF
		 }


	public InterruptionNotificiationHandler(Context ctx) {
		super(ctx);
	}	
	
	private int getNumOfRetries(Event event){
		try {
			EventVar retriesCount = null;
			retriesCount = event
					.getVar("DMA_VAR_DL_RETRY_COUNTER");
			return retriesCount.getValue();
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
			return 0;
		}
	}
	@Override
	protected Notification.Builder notificationHandler(Event event, int flowId)
		throws CancelNotif {
		String eventName = event.getName();
		
		Log.d(LOG_TAG, "notificationHandler=>event name: " + eventName);

		if (finishFlow(event))
			return null;
		// If DL resume counter > 1 we don't show any more notifications so the user
		// won't see a notification per retry
		if (getNumOfRetries(event) > 0)
			return null;
		
		String title = ctx.getString(R.string.dl_interruption_notif_title);
		String text = getText(event);		
	
		
		Notification.Builder notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.ic_notify_failure)
			.setContentTitle(title)
			.setContentText(text)
			.setOngoing(false)
			.setAutoCancel(true)
			.setTicker(title + "-" + text)
			.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));
		Log.d(LOG_TAG, "-notificationHandler=>text for notification: " + text);
		return notificationBuilder;
	}
	
	private String getText(Event event) {
		
		String text = String.format(
				ctx.getString(R.string.dl_interruption_notif));

		if (event.getName().equals("DMA_MSG_DNLD_FAILURE")) {
			EventVar network_reason_var = null;
			EventVar err_var = null;
			try {
				network_reason_var = event
						.getVar("DMA_VAR_NETWORK_UI_REASONS");
				err_var = event.getVar("DMA_VAR_ERROR");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				network_reason_var = null;
				err_var = null;
			}
			if (network_reason_var != null) {
				int network_interrupt_reason_val = network_reason_var.getValue();
				
				Log.d(LOG_TAG, "getText=>network_interrupt_reason = "
						+ network_interrupt_reason_val);
				
				if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_WIFI_ONLY_WIFI_OFF
						.ordinal()) {
					text = ctx.getString(R.string.wifi_only);
				} else if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_ROAMING
						.ordinal()) {
					text = ctx.getString(R.string.roaming_zone);
				} else if (network_interrupt_reason_val == NetworkUIReason.E_FAILURE_UI_NO_NETWORK
						.ordinal()) {
					text = ctx.getString(R.string.no_network);
				}
			}
			if (err_var != null) {
				if (err_var.getValue() == VdmError.PURGE_UPDATE.val)
				{
					text = ctx.getString(R.string.cancel_due_purge);
				}
			
				if (err_var.getValue() == VdmError.COMMS_HTTP_FORBIDDEN.val)
				{
					text = ctx.getString(R.string.url_expiration);
				}
				
			}
		} else if (event.getName().equals("DMA_MSG_SCOMO_DL_CANCELED_UI")) {
			text = ctx.getString(R.string.download_canceled);
		} else if (event.getName().equals("DMA_MSG_DL_INST_ERROR_UI")) {
			text = ctx.getString(R.string.user_interaction_timeout);
			
			EventVar err_var = null;
			try {
				err_var = event.getVar("DMA_VAR_ERROR");
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (err_var != null) {
				if (err_var.getValue() == VdmError.PURGE_UPDATE.val)
				{
					text = ctx.getString(R.string.cancel_due_purge);
				}
			}
		} 
		return text;
	}
	
	private boolean finishFlow(Event ev) {
		try {
			EventVar isRetry = ev.getVar("DMA_VAR_DL_WILL_RETRY");
			
			if (isRetry == null)
				return false;
			if (isRetry.getValue() == 0) {
				//Stopping the flow as DL resume has finished its work
				((ClientService)ctx).requestFinishFlow(1, false);
				return true;
			}
		} catch (EmptyStackException e) {
			Log.e(LOG_TAG, e.toString());
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
		return false;
	}
}
