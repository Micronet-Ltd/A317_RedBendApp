/*
 *******************************************************************************
 *
 * BatteryLowNotificationHandler.java
 *
 * Notifies the end-user that the update was interrupted by a low battery.
 * After user-confirmation, the business logic checks the battery level using
 * GetBatteryLevelHandler. If the battery level is too low, this class handles
 * background notification while the class BatteryLow handles foreground
 * notification.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI (background only)
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
import com.redbend.app.FlowManager;
import com.redbend.client.R;


/**
 * Display Charge Request notification
 */
public class BatteryLowNotificationHandler extends EventHandler {
	
	protected final String LOG_TAG = getClass().getSimpleName();

	public BatteryLowNotificationHandler(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
			throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");
			Notification.Builder builder = new Notification.Builder(ctx)
					.setSmallIcon(R.drawable.ic_notify_rb)
					.setOngoing(false)
					.setAutoCancel(true)
					.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));
			builder.setContentTitle(ctx.getString(R.string.charge_request_notification_title));
			builder.setContentText(ctx.getString(R.string.charge_request_notification_text));
			builder.setTicker(ctx.getString(R.string.charge_request_notification_ticker));

		Log.d(LOG_TAG, "-notificationHandler");
		return builder;
	}
}
