/*
 *******************************************************************************
 *
 * DilActivity.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.widget.TextView;

/**
 * DIL Activity Class
 */
@TargetApi(5)
abstract public class DilActivity extends Activity 
{
	public static final String eventMsg = "eventMsg";
	public static final String noBackgroundExtra = "noBackground";
	public static final String serviceName = "service";
	protected final String LOG_TAG = getClass().getSimpleName() + "(" + Integer.toHexString(hashCode()) + ")";
	private Class<?> serviceCls;
	private LinkedList<Message> msgsToSend = new LinkedList<Message>();

	private int flowId;
	
	private AlertDialog alert;

	/* 'true' if the screen UI is displayed, 'false' if the screen is loading and
	 * the screen UI is not yet updated (the temporary UI is shown) */
	private Boolean activeView; 
	private boolean startingNewActivity = false;
	
	/* 'true' if the activity shouldn't live in background */
	private boolean noBackground = false;
	
	private boolean finishingOnStop = false;

	/* Messenger for communicating with the service */
	Messenger mService = null;

	/* TODO saving the state of the activity when paused isn't implemented,
	 * according to expected behavior Android App manager could decide to 
	 * kill it, and afterwards return to it */

	/* Sets the active view upon activity onCreate and onRestart,
	 * (according to start parameter value) happens when the top 
	 * most activity changes in background (onRestart) */
	abstract protected void setActiveView(boolean start, Event receivedEvent);

	/*
	 * Handler of incoming messages from service.
	 */
	static class IncomingHandler extends Handler {

		private final WeakReference<DilActivity> activity;
		private final static String LOG_TAG = "DilActivity.Handler";

		public IncomingHandler(DilActivity activity) {
			this.activity = new WeakReference<DilActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			DilActivity a = activity.get();
			if (a == null)
				return;
			Log.d(LOG_TAG,"handleMessage: " + msg.what );
			switch (msg.what) {
			case SmmService.MSG_START_ACTIVITY:
				a.startActivity((Intent) msg.obj);
				a.startingNewActivity = true;
				a.finish();
				break;
			case SmmService.MSG_SEND_EVENT_TO_UI:
				a.getEvent((Intent) msg.obj, false);
				break;
			case SmmService.MSG_END_TASK:
				Log.d(LOG_TAG, "service requested finish, executing");
				a.finish();
				if ((Boolean)msg.obj) {
					/* avoid animations */
					a.overridePendingTransition(0, 0);
				}
				break;
			case SmmService.MSG_ACTIVITY_NEW_INTENT:
				a.activeView = false;
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/* Class for interacting with the main interface of the SMM service. */
	class SmmServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			synchronized (msgsToSend) {
				mService = new Messenger(service);

				Log.d(LOG_TAG, "Bind to service " + className.getShortClassName() + " successfully, client:" + 
						Integer.toHexString(mMessenger.hashCode()));
				try {
					Message msg = Message.obtain(null, SmmService.MSG_REGISTER_CLIENT + (flowId << 8));
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// there is no need to do anything here.
				}

				/* send messages that were tried to be sent before the connection,
				 * it is an extreme case, for example "finish" before connecting */
				if (!msgsToSend.isEmpty()) {
					Log.i(LOG_TAG, "Sending msgs that were sent before connection establishment");
					for (Message pendingMsg: msgsToSend)
						sendMsg(pendingMsg);
					msgsToSend.clear();
					
					/* additionally if there were msgs to send, then the service connection
					 * is kept active till we send them */
					if (hasFinished) {
						Log.i(LOG_TAG, "Unbinding service connection after pending msgs");
						unbindService(mConnection);
						mConnection = null;
					}
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(LOG_TAG, "Service unexpectedly closed the connection");
			mService = null;
		}
	}

	/**
	 * Handle the back button.
	 *
	 * Overridden so that the activity goes to the background, instead of being
	 * killed. */
	@Override
	public void onBackPressed() {
		Log.d(LOG_TAG, "Back button pressed, move task to background");
		if (!moveTaskToBack(true))
		{
			Log.d(LOG_TAG, "Moving task to background failed, performing " +
					"the default action");
			super.onBackPressed();
		}
	}

	/*
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	private ServiceConnection mConnection;
	private boolean hasFinished = false;

	private void sendMsg(Message msg)
	{
		synchronized (msgsToSend) {
			if (mService == null)
			{
				Log.d(LOG_TAG, "Skipping event, since the service connection isn't active, will send later");
				msgsToSend.add(msg);
				return;
			}
			try {
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void sendMsg(int what)
	{
		sendMsg(Message.obtain(null, what + (flowId << 8)));
	}

	private void sendMsg(int what, Object obj)
	{
		sendMsg(Message.obtain(null, what + (flowId << 8), obj));
	}

	private void doBindService()
	{
		Log.d(LOG_TAG, "Binding to service " + serviceCls.getName());
		bindService(new Intent(DilActivity.this, serviceCls), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	private void doUnbindService(boolean finishing)
	{
		if (hasFinished) {
			Log.i(LOG_TAG, "Activity has finished and already unregistered");
			return;
		}

		Log.d(LOG_TAG, "Unregistering from service");
		if (!finishing)
			sendMsg(SmmService.MSG_UNREGISTER_CLIENT, Boolean.valueOf(startingNewActivity));

		synchronized (msgsToSend) {
			// Detach our existing connection.
			if (msgsToSend.isEmpty())
				unbindService(mConnection);
			else
				Log.w(LOG_TAG, "There are still unsent messages at service disconnection, will disconnect later");
				
			hasFinished = true;
		}
	}

	@SuppressLint("UseValueOf")
	private void getEvent(Intent intent, boolean firstRun)
	{
		byte event[] = intent.getByteArrayExtra(eventMsg);

		if (event == null)
			return;
		
		noBackground = intent.getBooleanExtra(noBackgroundExtra, false);

		try {
			Event receivedEvent = new Event(event);

			if (!activeView) {
				setActiveView(firstRun, receivedEvent);
				activeView = Boolean.valueOf(true);
			}

			/* we are transmitting the event, _only_ after setting the view */
			newEvent(receivedEvent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		
		try {
			serviceCls = Class.forName(i.getStringExtra(serviceName));
		} catch (ClassNotFoundException e) {
			Log.d(LOG_TAG, "Error getting the service name the activity should connect to, closing");
			finish();
			return;
		} catch (NullPointerException e) {
		// This is a workaround for unwanted duplicate instance of DilActivity.
			Log.e(LOG_TAG, "NullPointerException, closing");
			finish();
			return;
		}

		flowId = i.getIntExtra(SmmService.flowIdExtra, 0);
		Log.d(LOG_TAG, "OnCreate, flowid:" + flowId);
		mConnection = new SmmServiceConnection();
		doBindService();
		activeView = null;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (activeView != null)
			return;
		activeView = Boolean.valueOf(false);
		getEvent(getIntent(), true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		if (alert != null && alert.isShowing())
			alert.dismiss();
		
		if (isFinishing())
		{
			Log.d(LOG_TAG, "onStop: Activity is finished, don't send pause");
			return;
		}
		Log.d(LOG_TAG, "onStop: Activity moved to background");
		sendMsg(SmmService.MSG_ACTIVITY_PAUSED);
		
		if (noBackground) {
			finishingOnStop = true;			
			finish();
		}
	}
	
	protected void stopActivity() {
		super.onStop();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(LOG_TAG, "Activity returned from background");
		sendMsg(SmmService.MSG_ACTIVITY_RESUMED);

		/* when the application comes
		 * from background while SmmService doing all ui mode updates
		 * the loading screen is shown */
		if (!activeView) {
			TextView tv = new TextView(this);
			tv.setText("Loading...");
			setContentView(tv);
		}
		
		if (alert != null)
			alert.show();
	}

	@Override
	protected void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");
		doUnbindService(false);
		super.onDestroy();
	}

	/* called on receiving a new Event for the same intent */
	protected void newEvent(Event receivedEvent) {
		Log.d(LOG_TAG, "Received event " + receivedEvent.getName());
	}

	protected final void sendEvent(Event ev)
	{
		Log.d(LOG_TAG, "Sending event " + ev.getName());
		sendMsg(SmmService.MSG_SEND_EVENT_TO_SM, ev);
	}

	/**
	 * Set an alarm.
	 *
	 * @param	alarmTime	The time until the alarm, in ms.
	 */
	public void setTimer(long alarmTime)
	{
		sendMsg(SmmService.MSG_SET_TIMER, Long.valueOf(alarmTime));
	}

	@Override
	public void finish() {
		super.finish();
		if (!startingNewActivity) {
			Log.i(LOG_TAG, "Sending finish msg to service");
			sendMsg(SmmService.MSG_ACTIVITY_FINISHED, Boolean.valueOf(finishingOnStop));
			doUnbindService(true);
		}
	}
	
	protected int getFlowId() {
		return flowId;
	}
	
	protected void showDialog(AlertDialog.Builder builder) {	
		alert = builder.create();
		alert.show();
	}
	
	protected void removeDialog() {
		if (alert != null && alert.isShowing())
			alert.cancel();
		alert = null;
	}
}
