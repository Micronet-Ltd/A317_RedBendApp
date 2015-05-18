/*
 *******************************************************************************
 *
 * DownloadConfirmNotificationHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.SmmService;
import com.redbend.client.ClientService;
import com.redbend.client.R;


/**
 * Display notification prompting the end-user to confirm or reject the
 * download.
 */
public class DownloadConfirmNotificationHandler extends EventHandler{

	private final String LOG_TAG = "DownloadConfirmNotificationHandler::";
	
	public DownloadConfirmNotificationHandler(Context ctx/*, NotificationManager notifManager*/) {
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
		
		int criticalMode = 0;
		try	{
			criticalMode = ev.getVar("DMA_VAR_SCOMO_CRITICAL").getValue();
		}
		catch (Exception e)	{
			e.printStackTrace();
		}
		
		if(criticalMode == 1) {
			Log.d(LOG_TAG, "criticalMode:" + criticalMode);
			return null;
		}
		Log.d(LOG_TAG, "notificationHandler:Notification.Builder notificationBuilder");	
		Notification.Builder notificationBuilder = new Notification.Builder(ctx)
			.setSmallIcon(R.drawable.dlandins_proceed)
			.setOngoing(true)
			.setAutoCancel(true)
			.setTicker(ctx.getString(R.string.scomo_dl_notification_ticker))
			.setContentTitle(ctx.getString(R.string.scomo_dl_notification_title))
			.setContentText(ctx.getString(R.string.scomo_dl_notification_text));
		

		Intent notificationIntent = new Intent(ctx, ClientService.class);

		try {
			byte[] e = new Event("DMA_MSG_SCOMO_NOTIFY_DL").toByteArray();			
			notificationIntent.putExtra(SmmService.startServiceMsgExtra, e);
			notificationBuilder.setContentIntent(PendingIntent.getService(ctx, 0, notificationIntent,
					PendingIntent.FLAG_UPDATE_CURRENT));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
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
