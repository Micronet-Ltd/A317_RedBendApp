/*
 *******************************************************************************
 *
 * FlowManager.java
 *
 * Logic of a flow: a flow is a set of event handlers that belong to the same
 * task. SmmService uses the FlowManager logic.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.util.EmptyStackException;
import java.util.NoSuchElementException;
import java.util.Stack;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class FlowManager {
	private Stack<Messenger> clients = new Stack<Messenger>();

	// notification icon id start offset
	private static final int BACKGROUND_NOTIF_ID = 100;
	private static final int REQUEST_ID_OFFSET = 100;

	private final String LOG_TAG = getClass().getSimpleName(); 

	private final int m_flowId;

	/** Variable that keeps whether there's an activity in the process of
	 *  being started. We are making sure that the next UI event's handling
	 *  is done only once the previous UI event successfully started the 
	 *  activity it requested to start */
	private boolean startingActivity = false;

	/* make sure we notify that activity has started only if somebody's waiting
	 * for it */
	private boolean waitingForActivity = false;

	/** Keeping the last component name, to detect whether the next intent is
	 * destined for the same component. */ 
	private ComponentName lastComponent;

	private Notification.Builder backgroundNotif;

	private int uiMode = SmmService.UI_MODE_BACKGROUND;

	private ComponentName rootComponentName;

	private Intent inactiveIntent, lastIntent;
	private Notification.Builder inactiveNotification;
	private boolean keepNotificationInForeground;
	
	private NotificationManager notificationManager;
	private ActivityManager activityManager;
	private final Messenger mMessenger;
	private Context ctx;

	public FlowManager(int m_flowId, Context ctx, Messenger mMessenger) {
		this.m_flowId = m_flowId;
		this.ctx = ctx;
		this.mMessenger = mMessenger;
		
		notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
		keepNotificationInForeground = false;
	}

	public int getFlowId() {
		return m_flowId;
	}

	public boolean isForeground() {
		return uiMode == SmmService.UI_MODE_FOREGROUND;
	}

	public boolean isBackground() {
		return uiMode == SmmService.UI_MODE_BACKGROUND;
	}

	public void setForeground() {
		uiMode = SmmService.UI_MODE_FOREGROUND;
		execInactiveIntent();
		if (!keepNotificationInForeground)
			notificationManager.cancel(BACKGROUND_NOTIF_ID + m_flowId);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void setBackground() {
		uiMode = SmmService.UI_MODE_BACKGROUND;
		execInactiveIntent();
		if (!isEmpty() && backgroundNotif != null)
			notificationManager.notify(BACKGROUND_NOTIF_ID + m_flowId, getBackgroundNotif());
		else if (inactiveNotification != null)
		{
			if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
				notificationManager.notify(BACKGROUND_NOTIF_ID + m_flowId, inactiveNotification.build());
			else
				notificationManager.notify(BACKGROUND_NOTIF_ID + m_flowId, inactiveNotification.getNotification());
		}
	}

	public int getUiMode() {
		return uiMode;
	}

	public final static PendingIntent getReturnToFgIntent(Context serviceCtx, int m_flowId) {
		return PendingIntent.getService(serviceCtx, REQUEST_ID_OFFSET + m_flowId,
			new Intent(serviceCtx, serviceCtx.getClass())
			.putExtra(SmmService.returnFromBackground, true)
			.putExtra(SmmService.flowIdExtra, m_flowId),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public void setBackgroundNotif(Context ctx, int icon, CharSequence backgroundTitle, CharSequence backgroundText) {
		backgroundNotif = new Notification.Builder(ctx)
		.setSmallIcon(icon)
		.setTicker(backgroundText)
		.setContentTitle(backgroundTitle)
		.setContentText(backgroundText)
		.setOngoing(true)
		.setContentIntent(getReturnToFgIntent(ctx, m_flowId));
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private Notification getBackgroundNotif() {
		if (backgroundNotif != null){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
				return backgroundNotif.build();
			return backgroundNotif.getNotification();
		}
		return null;
	}

	public int add(Messenger m) {
		synchronized(this) {
			clients.push(m);
			// if a client was added then we finish starting a new activity
			started();

			if (waitingForActivity) {
				FlowUtils.dLog(LOG_TAG, m_flowId, "Notifying the execution thread it can continue processing events");
				notify();
			}

			int size = size();

			/* if this is the start of a new task, remove the old notification */
			if (size == 1)
				notificationManager.cancel(BACKGROUND_NOTIF_ID + m_flowId);

			return size;
		}
	}

	private void waitForActivity() {
		boolean waited = false;

		synchronized(this) {
			waitingForActivity = true;
			while (startingActivity)
			{
				waited = true;
				FlowUtils.dLog(LOG_TAG, m_flowId, "Waiting for the next activity to start");
				try {
					wait();
				} catch (InterruptedException e) {
					// continue waiting in case the wait was interrupted
				}
			}
			waitingForActivity = false;
			if (waited)
				FlowUtils.dLog(LOG_TAG, m_flowId, "Finished waiting for the next activity");
		}
	}

	public int remove(Messenger m) {
		int i = clients.indexOf(m);

		if (i == -1)
			return -1;

		clients.remove(i);
		return i;
	}

	/** returns true if the provided intent is destined for the same component */
	public boolean compareAndSetLastComponent(Intent i) {
		ComponentName comp = lastComponent, newComp = i.getComponent();

		lastComponent = newComp;
		return comp != null && comp.compareTo(newComp) == 0;
	}

	public boolean isSameComponent(Intent i) {

		return lastComponent != null && lastComponent.compareTo(i.getComponent()) == 0;
	}

	public void reset(boolean keepIntent) {
		lastComponent = null;
		rootComponentName = null;		
		
		if (!keepIntent) {
			inactiveIntent = null;
			lastIntent = null;
			inactiveNotification = null;

			/* if there is some notification that isn't a generic one,
			 * then don't cancel it when finishing the task */
			keepNotificationInForeground = backgroundNotif == null;
		}
		else {
			Log.d(LOG_TAG, "Service informed of reset with request to keep the Intent");
			if (inactiveIntent == null) {
				Log.d(LOG_TAG, "No inactive intent was found when stopping the task, keep the last intent");
				inactiveIntent = lastIntent;
			}
			lastIntent = null;
		}
			
	}

	private Messenger get() {
		return clients.peek();
	}

	public void starting() {
		startingActivity = true;
	}

	public void started() {
		startingActivity = false;
	}

	public boolean isStarting() {
		return startingActivity;
	}

	public int size() {
		return clients.size();
	}

	public boolean isEmpty() {
		return clients.empty();
	}

	private Messenger getRoot() {
		try {
			return clients.firstElement();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public void setRootComponentName(ComponentName root) {
		rootComponentName = root;
		lastComponent = root;
	}

	public ComponentName getRootComponentName() {
		return rootComponentName;
	}
	
	private void sendMsg(Messenger client, Message msg)
	{
		try {
			msg.replyTo = mMessenger;
			client.send(msg);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendMsg(Messenger client, int what)
	{
		sendMsg(client, Message.obtain(null, what));
	}

	private void sendMsg(Messenger client, int what, Object obj)
	{
		sendMsg(client, Message.obtain(null, what, obj));
	}

	private void handleActivity(Intent i)
	{
		lastIntent = i;

		/* we assume that the activity to be started extends DilActivity Class */
		i.putExtra(DilActivity.serviceName, ctx.getClass().getName());
		i.putExtra(SmmService.flowIdExtra, m_flowId);

		/* This synchronization is used for activity synchronization, so the next activity is 
		 * handled only once the current activity has started. It is used also for the root
		 * activity creation. */
		waitForActivity();
		
		/* make sure that the called Activity inherits DilActivity, this is 
		 * needed for synchronizing the actual Activity start, the Activity
		 * to be started should bind to this service and send 
		 * MSG_REGISTER_CLIENT message */

		if (isEmpty())
		{
			/* if there's no activity, start a new task, the root activity is used
			 * when going to background and returning, all the next activities have 
			 * the flag FLAG_ACTIVITY_PREVIOUS_IS_TOP, which means they will be closed
			 * when starting the next activity */
			Intent root = i;

			FlowUtils.dLog(LOG_TAG, m_flowId, "No Root Activity, starting the root activity in a new task");
			root.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			root.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			root.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

			setRootComponentName(root.getComponent());
			starting();
			ctx.startActivity(root);
			return;
		}

		/* the top activity messenger is used for requesting to start the next 
		 * activity or forwarding the next event to the top activity */
		Messenger client = get();

		FlowUtils.dLog(LOG_TAG, m_flowId, "Sending new Intent Using Activity #" + size());

		/* checking whether the new event is destined for the same
		 * activity */
		if (compareAndSetLastComponent(i))
		{
			FlowUtils.dLog(LOG_TAG, m_flowId, "Sending event to the last activity " + i.getComponent().getClassName());
			sendMsg(client, SmmService.MSG_SEND_EVENT_TO_UI, i);
		}
		else /* starting a new activity */
		{
			/* XXX when the intent is sent through an activity that is in
			 * the background the new activity won't be started immediately,
			 * which means no further UI events will be handled, because of
			 * the starting activity sync mechanism,
			 * {@link #waitForActivity()} will block till the new activity
			 * is actually started and it's messenger is registered */
			i.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			FlowUtils.dLog(LOG_TAG, m_flowId, "Starting a new activity " + i.getComponent().getClassName());
			starting();
			sendMsg(client, SmmService.MSG_START_ACTIVITY, i);
		}
		if (isBackground()) {
			/* starting the activity while we are already at background, so
			 * return to foreground */
			FlowUtils.dLog(LOG_TAG, m_flowId, "returning to foreground, in order to display the new activity");
			returnToForeground();
		}
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void handle(EventHandler handler, Event ev, int ui) {
		FlowUtils.dLog(LOG_TAG, m_flowId, "+handle");
		try {
			handler.handle(ev, getFlowId());
		} catch (EventHandler.CancelNotif e) {
			notificationManager.cancel(BACKGROUND_NOTIF_ID + m_flowId);
			return;
		}
		
		if (handler.hasActivity()) {
			FlowUtils.dLog(LOG_TAG, m_flowId, "handle::handler.hasActivity(), ui:"+ui + " getUiMode():" + getUiMode());
			Intent i = handler.getIntent();
			
			if ((ui & getUiMode()) == 0) {
				FlowUtils.dLog(LOG_TAG, m_flowId,
					String.format("New intent for a background task, ui: %d uiMode: %d, isSameComp: %b",
					ui, getUiMode(), isSameComponent(i)));

				inactiveIntent = i;
				
				/* if there's running activity, inform it, that there's
				 * new intent that will be send once the ui mode will change */
				if (!isEmpty() && !isSameComponent(i)) {
					FlowUtils.dLog(LOG_TAG, m_flowId, "Informing the top activity of a new intent");
					sendMsg(get(), SmmService.MSG_ACTIVITY_NEW_INTENT);
				}
				else
					FlowUtils.dLog(LOG_TAG, m_flowId, "Saving the intent for the background activity");
			}
			else {
				if (isSameComponent(i)) {
					/* Sends also the event in case of top background activity, that receives
					 * a new event. This also supports the bizarre case of background activity,
					 * that was changed to other activity, but later in the flow re-displayed
					 * the first one, while the activity stays in the background */
					FlowUtils.dLog(LOG_TAG, m_flowId, "forwarding a new event to the activity that's in the background");
				}
				if ((ui & getUiMode()) != 0)
					handleActivity(i);
				inactiveIntent = null;
			}
		}
		else if (handler.hasNotification()) {
			Notification.Builder n = handler.getNotification();
			
			inactiveNotification = n;
			keepNotificationInForeground = (ui & SmmService.UI_MODE_FOREGROUND) != 0;
			if (n != null && (ui & getUiMode()) != 0)
			{
				Notification notif;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
					notif = n.build();
				else
					notif = n.getNotification();
				notificationManager.notify(BACKGROUND_NOTIF_ID + m_flowId, notif);
			}
		}
		FlowUtils.dLog(LOG_TAG, m_flowId, "-handle");
	}
	
	private void execInactiveIntent() {
		if (inactiveIntent != null) {
			handleActivity(inactiveIntent);
			inactiveIntent = null;
		}
	}
	
	public void returnToForeground()
	{
		/* setting "new task" even we didn't intent to start a new task,
		 * but to return to the "active" task */
		if (!isBackground()) {
			FlowUtils.dLog(LOG_TAG, m_flowId, "ERROR: The flow isn't currently in background");
			return;
		}
		
		for (ActivityManager.RunningTaskInfo i :  activityManager.getRunningTasks(1024)) {
			Log.d(LOG_TAG, "Found running task=" + i.baseActivity.flattenToShortString() + 
					", Top activity=" + i.topActivity.flattenToShortString() + 
					", taskId=" + i.id);
		}
		
		ComponentName cn = getRootComponentName();
		
		if (cn == null || isEmpty()) {
			FlowUtils.dLog(LOG_TAG, m_flowId, "No task running for the current flow");
			setForeground();
			return;
		}
		
		Intent root = new Intent();

		root.putExtra(DilActivity.serviceName, ctx.getClass().getName());
		root.putExtra(SmmService.flowIdExtra, m_flowId);

		root.setComponent(cn);
		FlowUtils.dLog(LOG_TAG, getFlowId(), "The application returns to foreground, " + cn.flattenToShortString());
		root.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(root);
	}

	public void removeRoot(Messenger client) {
		/* we want to make sure that while the root activity is shutting
		 * down no other intent will be forwarded (to be displayed) to it */
		Messenger root = getRoot();

		// if the activity was a single one, then it was the root
		if (root == null || root == client) {
			FlowUtils.dLog(LOG_TAG, m_flowId, "No \"root\" activity, when finishing the task");
		    uiMode = SmmService.UI_MODE_BACKGROUND;
			return;
		}
		remove(root);

		FlowUtils.dLog(LOG_TAG, m_flowId, "Activity has finished, closing also the task root");
		sendMsg(root, SmmService.MSG_END_TASK, Boolean.TRUE); 
	}

	public void requestFinishFlow(boolean noTransition) {
		FlowUtils.dLog(LOG_TAG, m_flowId, "Request to finish the flow, dismiss notification");
		notificationManager.cancel(BACKGROUND_NOTIF_ID + m_flowId);
		try {
			sendMsg(get(), SmmService.MSG_END_TASK, Boolean.valueOf(noTransition));
		}
		catch (EmptyStackException e) {
			FlowUtils.dLog(LOG_TAG, m_flowId, "Tried to finish a flow without an activity");
		}
	}
}
