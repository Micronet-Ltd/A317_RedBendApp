/*
 *******************************************************************************
 *
 * GcmReceiver.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmReceive;

/**
 * Manage GCM registration. Registration request is handled using
 * \ref GcmHandler.
 */
public abstract class GcmReceiver extends SmmReceive
{
	private static final String NOTIF_PREFS_NAME = "notificationPrefs";
	private static final String RETRY_TIME = "retry_time";
	private static final String NOTIF_ID = "notif_id";
	private static final String NOTIF_TS = "notif_time_stamp";
	private static final String SHARED_PREFS_SENDER_ID = "sender_id";
	private static final String SHARED_PREFS_REG_ID = "reg_id";
	public static final String INTENT_EXTRA_SENDER_ID = "sender_id";
	public static final String INTENT_EXTRA_REG_ID = "reg_id";
	private static final String SESSION_FORCE_CANCEL = "dmSession.lawmo";
	
	private static final String TOKEN = Long.toBinaryString(new Random().nextLong());
	private SharedPreferences notifPrefs;
	private SharedPreferences.Editor editor;
	private Class<?> serviceClass;
	
	abstract protected Class<?> getServiceClass(); 
	
	// Actions used by this receiver
    private static final String GCM_REGISTER_RETRY = "com.redbend.gcm.register.retry";	
	public static final String GCM_REGISTER_REQUEST = "com.redbend.gcm.register.request";
	public static final String GCM_UNREGISTER_REQUEST = "com.redbend.gcm.unregister.request";
	private static final String ACCOUNT_CHANGED = "android.accounts.LOGIN_ACCOUNTS_CHANGED";

	// Actions received from Google service
	private static final String GCM_REGISTRATION_RESPONSE = "com.google.android.c2dm.intent.REGISTRATION";
	private static final String GCM_RECEIVE_MSG = "com.google.android.c2dm.intent.RECEIVE";
    
    private static int retryCounter = 0;
    
    private String getPref(String key)
    {
		return notifPrefs.getString(key, null);
    }
    
    private boolean setPref(String key, String val)
    {
    	editor.putString(key, val);
		return editor.commit();
    }
    
	private boolean removePref(String key)
    {
    	editor.remove(key);
		return editor.commit();
    }
   
	@Override
	public void onReceive(Context ctx, Intent intent)
	{		
		String action = intent.getAction();
		
		serviceClass = getServiceClass();
		
		Log.d(LOG_TAG, "onReceive command: " + action);
		if (notifPrefs == null) {
			notifPrefs = ctx.getSharedPreferences(NOTIF_PREFS_NAME, 0);
			editor = notifPrefs.edit();
		}
			
		// check for version that's below JELLY_BEAN (16)
		if ((action.equals(ACCOUNT_CHANGED) && Build.VERSION.SDK_INT < 16)) {
			AccountManager am = (AccountManager) ctx.getSystemService(Context.ACCOUNT_SERVICE);
			Account accounts[] = am.getAccountsByType("com.google");
			
			Log.d(LOG_TAG, "onReceive accounts.length  =  " + accounts.length);			
			
			/* in case android api is less than 16 and there is no google account registered - nofity
			   about unregistered state. */
			if (accounts.length == 0){
				sendEvent(ctx, serviceClass, new Event("DMA_MSG_NET_NOTIF_UNREGIST"));
				removePref(SHARED_PREFS_REG_ID);
				Log.d(LOG_TAG, "onReceive, sent UNREGISTRATION event");
			}
            // in case the device registered to the first account register to GCM service
			else if (accounts.length == 1){
                resetRetryCounter();		
				registerToNotificationService(ctx, getPref(SHARED_PREFS_SENDER_ID));
			}
            return;
		}
		else if (action.equals(GCM_REGISTER_REQUEST)){
			/* handle registeration request from GCM bl*/
            resetRetryCounter();	
            String senderId = intent.getStringExtra(INTENT_EXTRA_SENDER_ID);
            String regId = intent.getStringExtra(INTENT_EXTRA_REG_ID);
            Log.d(LOG_TAG,"action: "+ GCM_REGISTER_REQUEST + "SenderId:" + senderId + "RefId: " + regId);            
            Log.d(LOG_TAG,"Sender id is:" + senderId);
	
			/* If the requester already has the same registration data that we have, we
			   don't need to do anything. */
			if (senderId != null && senderId.equals(getPref(SHARED_PREFS_SENDER_ID)) &&
				regId != null && regId.equals(getPref(SHARED_PREFS_REG_ID)))
			{
				sendNotifEvent(ctx, regId);
			}
			else if ( senderId != null && setPref(SHARED_PREFS_SENDER_ID, senderId))
			{
				registerToNotificationService(ctx, senderId);
			}
			else
			{
				Log.e(LOG_TAG, "onReceive, could not save sender ID! not registring to GCM");
				sendGcmFailedEvent(ctx);
			}
		}
		else if (action.equals(GCM_UNREGISTER_REQUEST)){
			/* handle un-registeration request from GCM bl*/
			/* registeraytion dsata must be the same */
	        String senderId = intent.getStringExtra(INTENT_EXTRA_SENDER_ID);
	        String regId = intent.getStringExtra(INTENT_EXTRA_REG_ID);
	        Log.d(LOG_TAG,"action: "+ GCM_UNREGISTER_REQUEST + "SenderId:" + senderId + "RefId: " + regId);     
	        Log.d(LOG_TAG,"Sender id is:" + senderId);
			/* The register data must be the same as the current registeration data 
			 * otherwise - do nothing */
			if (senderId != null && senderId.equals(getPref(SHARED_PREFS_SENDER_ID)) &&
				regId != null && regId.equals(getPref(SHARED_PREFS_REG_ID))) {
				unRegisterFromNotificationService(ctx);
			}
			else {
				Log.d(LOG_TAG, "onReceive, unregist data not equal to current regist data. No unregisteration done");
				sendGcmFailedEvent(ctx);
			}
		}
		/* handle response to reg request */
		else if (action.equals(GCM_REGISTRATION_RESPONSE))
			handleRegistrationResponse(ctx, intent);
		else if (action.equals(GCM_REGISTER_RETRY))
			handleRetry(ctx, intent, getPref(SHARED_PREFS_SENDER_ID));
		else if (action.equals(GCM_RECEIVE_MSG))
			handleMessage(ctx, intent);
		else
			sendGcmFailedEvent(ctx);
		
		setResult(Activity.RESULT_OK, null, null);
	}
	
	private void handleMessage(Context context, Intent intent) {
		String data;
		Bundle bundle = intent.getExtras();	
		
		/* The data payload of the message */
		data = (String) bundle.get("type");
		Log.d(LOG_TAG, "handleMessage, data=" + data);
		
		// generates a system notification to start a DM session
		Event ev = new Event("DMA_MSG_NET_NOTIFICATION");
		int forceCancel = data.equals(SESSION_FORCE_CANCEL) ? 1 : 0;
		ev.addVar(new EventVar("DMA_VAR_FORCE_CANCEL", forceCancel));
		sendEvent(context, serviceClass, ev);
	}
	
	private void registerToNotificationService(Context ctx, String senderId) {
		if (senderId == null)
		{
			Log.e(LOG_TAG, "registerToNotificationService: No sender ID exist, can't register to GCM");
			return;
		}

		printNotifTimeStamp();
		Intent registrationIntent = new Intent(
				"com.google.android.c2dm.intent.REGISTER");
		// sets the app name in the intent
		registrationIntent.putExtra("app",
				PendingIntent.getBroadcast(ctx, 0, new Intent(), 0));
		registrationIntent.putExtra("sender", senderId);
		ctx.startService(registrationIntent);
		Log.v(LOG_TAG, "registerToNotificationService: Registering GCM");
	}

	private void unRegisterFromNotificationService(Context ctx) {

		printNotifTimeStamp();
		Intent unRegistrationIntent = new Intent(
				"com.google.android.c2dm.intent.UNREGISTER");
		// sets the app name in the intent
		unRegistrationIntent.putExtra("app",
				PendingIntent.getBroadcast(ctx, 0, new Intent(), 0));
		ctx.startService(unRegistrationIntent);
		Log.v(LOG_TAG, "unRegisterFromNotificationService: sending unregister to GCM");
	}
	
	private void sendNotifEvent(Context ctx, String inRegistrationId)
	{
		Log.d(LOG_TAG, "handleRegistrationResponse, " + NOTIF_ID + "=" + inRegistrationId);

		// in case there is a new registration id replace the id
		sendEvent(ctx, serviceClass, new Event("DMA_MSG_NET_NOTIF_REGIST")			
			.addVar(new EventVar("DMA_VAR_NOTIF_REG_ID", inRegistrationId)));
	}
	
	private void sendGcmFailedEvent(Context ctx)
	{
		sendEvent(ctx, serviceClass, new Event("DMA_MSG_NET_GCM_FAILED"));
	}
	
	private void handleRegistrationResponse(Context ctx, Intent intent) {
		String registrationId = intent.getStringExtra("registration_id");
		String error = intent.getStringExtra("error");
		String unregistered = intent.getStringExtra("unregistered");
		
		// received registration Id from gcm server
		if (registrationId != null) {
			String regId = registrationId;
			sendNotifEvent(ctx, regId);
			saveNotifTimeStamp(regId);
			resetBackoffTimeMs();
			setPref(SHARED_PREFS_REG_ID, regId);
			Log.d(LOG_TAG, "handleRegistrationResponse, sent REGISTRATION event");
		}

		// unregistration from gcm server succeeded
		if (unregistered != null) {
			Log.d(LOG_TAG, "handleRegistrationResponse, unregistered=" + unregistered);

			sendEvent(ctx, serviceClass, new Event("DMA_MSG_NET_NOTIF_UNREGIST")
					.addVar(new EventVar("DMA_VAR_NOTIF_REG_ID", "")));
			removePref(SHARED_PREFS_REG_ID);
			Log.d(LOG_TAG, "handleRegistrationResponse, sent UNREGISTRATION event");
		}

		// last operation (registration or unregistration) succeeded
		if (error == null)
			return;
		
		Log.w(LOG_TAG, "Received error: " + error);
		
		/* in case of SERVICE_NOT_AVAILABLE or AUTHENTICATION_FAILED retry *
		 * in case of PHONE_REGISTRATION_ERROR retry only 3 times *
         * otherwise send unregistration event to the SMM */
		if (!"SERVICE_NOT_AVAILABLE".equals(error) && !"AUTHENTICATION_FAILED".equals(error)
				&& !("PHONE_REGISTRATION_ERROR".equals(error) && isDoRetry())) {
			// notify (DM) server that the client isn't registered to GCM
			sendEvent(ctx, serviceClass, new Event("DMA_MSG_NET_NOTIF_UNREGIST"));
			removePref(SHARED_PREFS_REG_ID);

			return;
		}
			
		Log.w(LOG_TAG, "handleRegistrationResponse, retry");
		long backoffTimeMs = getBackoffTimeMs();
		long nextAttempt = SystemClock.elapsedRealtime()
				+ backoffTimeMs;
		Intent retryIntent = new Intent(ctx.getApplicationContext(), GcmReceiver.class);
		retryIntent.setAction(GCM_REGISTER_RETRY).putExtra("token", TOKEN);
		PendingIntent retryPendingIntent = PendingIntent.getBroadcast(ctx, 
				0, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.ELAPSED_REALTIME, nextAttempt, retryPendingIntent);
		backoffTimeMs *= 2; // Next retry should wait longer.
		setBackoffTimeMs(backoffTimeMs);
	}

	public void handleRetry(Context ctx, Intent intent, String senderId) {
		String token = intent.getStringExtra("token");
		if (token != null && token.equals(TOKEN)) {
			Log.d(LOG_TAG, "handleRetry: RETRY-registerToNotificationService()");
			registerToNotificationService(ctx, senderId);
		} else
			Log.d(LOG_TAG, "handleRetry: RETRY-the token is incorrect TOKEN = " + TOKEN + " token = " + token);
	}

	private void resetBackoffTimeMs() {
		editor.remove(RETRY_TIME);
		editor.apply();
	}

	private long getBackoffTimeMs() {
		// first retry will be after 5 secs (time in ms)
		long retVal = notifPrefs.getLong(RETRY_TIME, 30000);
		Log.d(LOG_TAG, "getBackoffTimeMs retVal=" + retVal);
		return retVal;
	}

	private void setBackoffTimeMs(long time) {
		Log.d(LOG_TAG, "setBackoffTimeMs time=" + time);
		editor.putLong(RETRY_TIME, time);
		editor.apply();
	}

	private void printNotifTimeStamp() {
		String ts = notifPrefs.getString(NOTIF_TS, null);
		Log.d(LOG_TAG, "Previous GCM registration intent was received in " + ts);
	}

	private void saveNotifTimeStamp(String regId) {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(Calendar.getInstance().getTime());
		Log.d(LOG_TAG, "saveNotifRegInPS: regId=" + regId + " timeStamp=" + timeStamp);
		editor.putString(NOTIF_TS, timeStamp);
		editor.apply();
	}
	
    // returns the saved retry counter and increase it by one
	private static boolean isDoRetry() {
		return (retryCounter++ < 3);
	}
	
	private static void resetRetryCounter() {
		retryCounter = 0;
	}
}
