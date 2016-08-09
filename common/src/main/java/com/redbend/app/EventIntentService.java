/*
 *******************************************************************************
 *
 * EventIntentService.java
 *
 * Generates Android Intents when receiving DIL events, and generates BL events
 * when receiving Android Intents.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.IOException;
import java.util.TreeSet;

import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import com.redbend.app.Event;

abstract public class EventIntentService extends Service
{
	private static final String LOG_TAG = "EventIntentService";
	public static final String PERMISSION = "com.redbend.permission.EVENT_INTENT";
	private static final String receiverType = "com.redbend.camefrombroadcast";

	private TreeSet<String> uiEventsFilter = new TreeSet<String>();
	private TreeSet<String> eventsFilter = new TreeSet<String>();

	private BroadcastReceiver m_eventReceiver;
	IntentFilter incomingActions = new IntentFilter();

	private static class EventReceiver extends WakefulBroadcastReceiver {
		private final ComponentName serviceComponent;

		public EventReceiver(Class<?> serviceClass) {
			serviceComponent = new ComponentName(serviceClass.getPackage().getName(), serviceClass.getName());
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			intent.setComponent(serviceComponent);
			intent.setType(receiverType);
			startWakefulService(context, intent);
			if (isOrderedBroadcast())
				setResultCode(Activity.RESULT_OK);
		}
	}	
	
	private BroadcastReceiver startComplete = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			start();
		}
	};

	private void printVersion()
	{
		String version;

		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			version = "unknown";
		}
		Log.d(LOG_TAG, " **** Red Bend Software EventIntentService Version: " + version + " ****");
	}

	/* method that need to be implemented how an event will be forwarded to SMM */
	abstract protected void sendEvent(Event ev);
	
	// method that need to be implemented how the communication with the SMM is started */
	abstract protected void start();
	
	/* method that need to be implemented how the communication with the SMM is closed */
	abstract protected void shutdown();
	
	@SuppressLint("InlinedApi")
	protected void register(String intentStartAction) {
		registerReceiver(m_shutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
		
		initEventReceiver();
		Intent startIntent = new Intent(intentStartAction)
			.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
			startIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		sendOrderedBroadcast(startIntent, PERMISSION, startComplete, null, Activity.RESULT_FIRST_USER, null, null);
	}

	private final BroadcastReceiver m_shutdownReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(LOG_TAG, "System in shutdown, stopping the service");
			shutdown();
			stopSelf();
		}
	};

	@Override
	public void onCreate()
	{
		super.onCreate();		
		printVersion();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(m_eventReceiver);
		unregisterReceiver(m_shutdownReceiver);
		Log.d(LOG_TAG, "-onDestroy");
	}

	/** initialize BroadcastReceiver that receives BL Events,
	 * need to be called after adding all events */
	protected void initEventReceiver() {
		 m_eventReceiver = new EventReceiver(getClass());
		 registerReceiver(m_eventReceiver, incomingActions, PERMISSION, null);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	protected void addEventToSmm(String eventName) {
		Log.d(LOG_TAG, "addEventToSmm::Event " + eventName);
		eventsFilter.add(eventName);
		incomingActions.addAction(Event.intentActionPrefix + eventName);
	}

	protected void addEventFromSmm(String eventName) {
		Log.d(LOG_TAG, "addEventFromSmm::UI-Event " + eventName);		
		uiEventsFilter.add(eventName);
	}

	/* methods that return true if the event should be forwarded */
	protected boolean filterEventFromSmm(String eventName) {
		return uiEventsFilter.contains(eventName);
	}

	protected boolean filterEventToSmm(String eventName) {
		return eventsFilter.contains(eventName);
	}

	@SuppressLint("InlinedApi")
	protected void recvEvent(Event ev) {
		if (!filterEventFromSmm(ev.getName())) {
			Log.i(LOG_TAG, "Incoming Event " + ev.getName() + " filtered out");
			return;
		}
		
		Intent i = ev.createIntent();
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
			i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
		Log.i(LOG_TAG, "Received event " + ev.getName());
		sendBroadcast(i, PERMISSION);
	}

	/** Returns true if the event has been processed, and shouldn't be sent.
	 * @param ev The event that has been received
	 * @return True if the event has been processed and shouldn't be transmitted
	 */
	protected boolean processEvent(Event ev)
	{
		return false;
	}
	
	/**workaround for: service was killed by swipe application
	*out and was not restored as a sticky (Android 4.4, 4.4.2 bug)
	*/
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.i(LOG_TAG, "+onTaskRemoved");
	    Intent restartServiceIntent = new Intent(getApplicationContext(),
	            this.getClass());
	    restartServiceIntent.setPackage(getPackageName());

	    PendingIntent restartServicePendingIntent = PendingIntent.getService(
	            getApplicationContext(), 1, restartServiceIntent,
	            PendingIntent.FLAG_ONE_SHOT);
	    AlarmManager alarmService = (AlarmManager) getApplicationContext()
	            .getSystemService(Context.ALARM_SERVICE);
	    alarmService.set(AlarmManager.ELAPSED_REALTIME,
	            SystemClock.elapsedRealtime() + 1000,
	            restartServicePendingIntent);

	    Log.i(LOG_TAG, "-onTaskRemoved");
	    super.onTaskRemoved(rootIntent);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOG_TAG, "onStartCommand received intent: " + intent);
		if (intent == null || intent.getAction() == null)
			return START_STICKY;

		Event ev;
		boolean startedFromReceiver = receiverType.equals(intent.getType());

		try {
			ev = new Event(intent);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error receiving " + intent + e.getMessage());
			if (startedFromReceiver)
				WakefulBroadcastReceiver.completeWakefulIntent(intent);
			return START_STICKY;
		}

		String eventName = ev.getName();

		if (processEvent(ev)) {
			Log.i(LOG_TAG, "Outgoing Event " + eventName + " was processed and won't be sent.");
		}
		else if (!filterEventToSmm(eventName)) {
			Log.i(LOG_TAG, "Outgoing Event " + eventName + " was filtered out.");
		}
		else {
			Log.i(LOG_TAG, "Outgoing Event " + eventName);
			sendEvent(ev);
		}
		if (startedFromReceiver) {
			Log.i(LOG_TAG, "Finishing wakefull Intent");
			WakefulBroadcastReceiver.completeWakefulIntent(intent);
		}
		return START_STICKY;
	}
}
