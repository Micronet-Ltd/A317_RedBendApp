/*
 *******************************************************************************
 *
 * LawmoUnLockResultNotification.java
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
import com.redbend.app.FlowManager;
import com.redbend.client.R;

/**
 * Display a UnLock result notification.
 */
public class LawmoUnLockResultNotification extends EventHandler {
	private final String LOG_TAG = getClass().getSimpleName();
	
	public LawmoUnLockResultNotification(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		
		Notification.Builder notificationBuilder = null;
		String showText = "";
		Log.d(LOG_TAG, "event: " + ev.getName());
		
		if ("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS".equals(ev.getName())) {
			showText = ctx.getResources().getString(R.string.device_unlocked);
		}
		else
		{
			showText = ctx.getResources().getString(R.string.device_cannot_be_unlocked);
		}
		
		notificationBuilder = 
				new Notification.Builder(ctx)
					.setSmallIcon(R.drawable.notif_icon)
					.setTicker(showText)
					.setWhen(0)
					.setOngoing(false)
					.setAutoCancel(true)
					.setContentTitle(ctx.getString(R.string.lock_result))
					.setContentText(showText)
					.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));

		Log.d(LOG_TAG, "description: " + showText);
		return notificationBuilder;
	}
}
