/*
 *******************************************************************************
 *
 * DownloadConfirmNotificationHandler.java
 *
 * Displays a notification prompting the end-user to that a download is
 * available.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_NOTIFY_DL_UI (not silent update only, background only)
 *
 * Sends BL events:
 * DMA_MSG_SCOMO_NOTIFY_DL
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;


import java.io.IOException;
import android.app.PendingIntent;
import android.content.Intent;
import com.redbend.app.SmmService;
import com.redbend.client.ClientService;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.client.R;


/**
 * Display notification prompting the end-user to confirm or reject the
 * download.
 */
public class DownloadConfirmNotificationHandler extends EventHandler{

	private final String LOG_TAG = "DownloadConfirmNotificationHandler::";
	
	public DownloadConfirmNotificationHandler(Context ctx) {
		super(ctx);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");		
		if (!ev.getName().equals("DMA_MSG_SCOMO_NOTIFY_DL_UI")) {
			throw new CancelNotif();
		}

		Log.d(LOG_TAG, "notificationHandler:Notification.Builder notificationBuilder");	
		Notification.Builder notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.ic_notify_download)
			.setOngoing(true)
			.setAutoCancel(true)
			.setTicker(ctx.getString(R.string.scomo_dl_notification_ticker))
			.setContentTitle(ctx.getString(R.string.scomo_dl_notification_title))
			.setContentText(ctx.getString(R.string.scomo_dl_notification_text));
		


		try {
			Intent notificationIntent = new Intent(ctx, ClientService.class);
			byte[] e = new Event("DMA_MSG_SCOMO_NOTIFY_DL").toByteArray();
			//Add extra for send event to SMM, flowId, also for returning flow activity to foreground
			notificationIntent.putExtra(SmmService.flowIdExtra, flowId);
			notificationIntent.putExtra(SmmService.startServiceMsgExtra, e);
			notificationIntent.putExtra(SmmService.returnFromBackground, true);
			notificationBuilder.setContentIntent(PendingIntent.getService(ctx, 0, notificationIntent,
					PendingIntent.FLAG_UPDATE_CURRENT));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		//notificationBuilder.setWhen(0).setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));

		Notification notif = null;		
		if (Build.VERSION.SDK_INT < 17) {
			notif = notificationBuilder.getNotification();
		 } 
		 else {
			 notif = notificationBuilder.build();
		 }

		notif.flags = Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;
		Log.d(LOG_TAG, "-notificationHandler");	
		return notificationBuilder;
	}
}
