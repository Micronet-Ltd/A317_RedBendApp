/*
 *******************************************************************************
 *
 * InstallConfirmNotificationHandler.java
 *
 * Displays an installation confirmation notification.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_INS_CONFIRM_UI if device is initiated (not silent update only, background only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.client.R;
import com.redbend.app.FlowManager;

/**
 * Display an installation confirmation notification.
 */
public class InstallConfirmNotificationHandler extends EventHandler {
	
	private final String LOG_TAG = "InstallConfirmNotificationHandler::";

	public InstallConfirmNotificationHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");	
		Notification.Builder notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.ic_notify_success)
			.setTicker(ctx.getString(R.string.scomo_install_waiting_notif_ticker))
			.setContentTitle(ctx.getString(R.string.scomo_install_waiting_notif_title))
			.setContentText(ctx.getString(R.string.scomo_install_waiting_notif_text))
			.setOngoing(true)
			.setAutoCancel(true)
			.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));

		return notificationBuilder;
	}
}
