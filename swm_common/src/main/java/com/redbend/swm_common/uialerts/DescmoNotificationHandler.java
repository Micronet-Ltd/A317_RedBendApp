/*
 *******************************************************************************
 *
 * DescmoNotificationHandler.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.uialerts;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.swm_common.R;
import com.redbend.app.FlowManager;

/**
 * Display notification when admin permissions are required.
 */
public class DescmoNotificationHandler extends EventHandler {
	
	private final String LOG_TAG = "DescmoNotificationHandler";

	public DescmoNotificationHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");	
		Notification.Builder notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.common_dlandins_complete)
			.setTicker(ctx.getString(R.string.common_descmo_missing_admin_permissions))
			.setContentTitle(ctx.getString(R.string.common_descmo_title_missing_admin_permissions))
			.setContentText(ctx.getString(R.string.common_descmo_text_missing_admin_permissions))
			.setOngoing(true)
			.setAutoCancel(true)
			.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));

		return notificationBuilder;
	}
}
