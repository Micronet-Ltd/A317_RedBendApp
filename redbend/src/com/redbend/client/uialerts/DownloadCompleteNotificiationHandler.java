/*
 *******************************************************************************
 *
 * NotificiationHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import android.app.*;

import com.redbend.app.*;

import android.content.Context;
import android.util.Log;

import com.redbend.client.R;

/**
 * Display a download complete notification.
 */
public class DownloadCompleteNotificiationHandler extends EventHandler {
	
	private final String LOG_TAG = getClass().getSimpleName();
		
	public DownloadCompleteNotificiationHandler(Context ctx) {
		super(ctx);
	}	

	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		
		Notification.Builder notificationBuilder = null;
		String eventName = ev.getName();
		
		if (eventName != null && eventName.equals("DMA_MSG_SET_DL_COMPLETED_ICON")) {
			Log.d(LOG_TAG, "notificationHandler: event DMA_MSG_SET_DL_COMPLETED_ICON");
			
		notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.dlandins_complete)
			.setTicker(ctx.getString(R.string.scomo_dl_completed_ticker))
			.setContentTitle(ctx.getString(R.string.scomo_dl_notification_title))
			.setContentText(ctx.getString(R.string.scomo_dl_ok_install_waiting))
			.setAutoCancel(true)
			.setWhen(0).setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));
		}
		Log.d(LOG_TAG, "-notificationHandler");
		return notificationBuilder;
	}
}
