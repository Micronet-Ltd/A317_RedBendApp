/*
 *******************************************************************************
 *
 * EventHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;

public abstract class EventHandler {
	public class CancelNotif extends Exception {

		private static final long serialVersionUID = 1L;

	}

	protected Context ctx;
	private Intent intent;
	private Notification.Builder notif;
	
	public EventHandler(Context ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * @param ev  Event to Handle
	 * @return an Activity to display
	 */
	protected Intent activityHandler(Event ev) {
		return null;
	}
	
	/**
	 * @param ev  Event to Handle
	 * @param flowId  flow for which to display the notification
	 * @return a Notification.Builder for notification to show
	 * @throws CancelNotif  In order to remove the notification, throw it
	 */
	protected Notification.Builder notificationHandler(Event ev, int flowId) throws CancelNotif {
		return null;
	}
	
	/**
	 * @param ev  Event to Handle
	 */
	protected void genericHandler(Event ev) {
		// empty
	}
	
	public final void handle(Event ev, int flowId) throws CancelNotif {
		intent = activityHandler(ev);
		if (intent != null)
			return;
		
		notif = notificationHandler(ev, flowId);
		if (notif != null)
			return;
		
		genericHandler(ev);
	}
	
	public final void cancelNotif() throws CancelNotif {
		throw new CancelNotif();
	}
	
	public final boolean hasActivity() {
		return intent != null;
	}
	
	public final boolean hasNotification() {
		return notif != null;
	}
	
	public final Intent getIntent() {
		return intent;
	}
	
	public final Notification.Builder getNotification() {
		return notif;
	}
}

