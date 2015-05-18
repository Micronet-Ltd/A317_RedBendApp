/*
 *******************************************************************************
 *
 * InstallConfirmNotificationHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
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
			.setSmallIcon(R.drawable.dlandins_complete)
			.setTicker(ctx.getString(R.string.scomo_dl_ok_install_waiting))
			.setContentTitle(ctx.getString(R.string.scomo_dl_ok_install_waiting))
			.setOngoing(true)
			.setAutoCancel(true)
			.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));

		return notificationBuilder;
	}
}
