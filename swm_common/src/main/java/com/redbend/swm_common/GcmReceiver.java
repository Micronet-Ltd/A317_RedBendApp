/*
 *******************************************************************************
 *
 * GcmReceiver.java
 *
 * Manages GCM registration. Receives GCM messages from Google.
 *
 * Sends BL events:
 * DMA_MSG_NET_NOTIFICATION on GCM message.
 * DMA_MSG_NET_NOTIF_REGIST on new registration
 * DMA_MSG_NET_NOTIF_UNREGIST on unregistration
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
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
	private static final String NOTIF_ID = "notif_id";
	private static final String NOTIF_TS = "notif_time_stamp";
	private static final String SHARED_PREFS_SENDER_ID = "sender_id";
	private static final String SHARED_PREFS_REG_ID = "reg_id";
	public static final String INTENT_EXTRA_SENDER_ID = "sender_id";
	public static final String INTENT_EXTRA_REG_ID = "reg_id";
	private static final String SESSION_PUSH_LAWMO = "dmSession.lawmo";
	private static final String SESSION_PURGE_UPDATE = "dmSession.invalidDp";
	
	private SharedPreferences notifPrefs;
	private SharedPreferences.Editor editor;
	private Class<?> serviceClass;
	
	abstract protected Class<?> getServiceClass(); 
	
	// Actions used by this receiver
	public static final String GCM_REGISTER_REQUEST = "com.redbend.gcm.register.request";
	public static final String GCM_UNREGISTER_REQUEST = "com.redbend.gcm.unregister.request";
	private static final String ACCOUNT_CHANGED = "android.accounts.LOGIN_ACCOUNTS_CHANGED";

	// Actions received from Google service
	private static final String GCM_REGISTRATION_RESPONSE = "com.google.android.c2dm.intent.REGISTRATION";
	private static final String GCM_RECEIVE_MSG = "com.google.android.c2dm.intent.RECEIVE";
    
    public static final int CANCEL_SESSION_NO_CANCEL = 0;
    public static final int CANCEL_SESSION_PUSH_LAWMO = 1;
    public static final int CANCEL_SESSION_PURGE_UPDATE = 2;
    
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
				registerToNotificationService(ctx, getPref(SHARED_PREFS_SENDER_ID));
			}
            return;
		}
		else if (action.equals(GCM_REGISTER_REQUEST)){
			/* handle registeration request from GCM bl*/
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
		else if (action.equals(GCM_REGISTRATION_RESPONSE)) {
			handleRegistrationResponse(ctx, intent);
			setResult(Activity.RESULT_OK, null, null);
		}
		else if (action.equals(GCM_RECEIVE_MSG)) {
			handleMessage(ctx, intent);
			setResult(Activity.RESULT_OK, null, null);
		}
		else
			sendGcmFailedEvent(ctx);
	}
	
	private void handleMessage(Context context, Intent intent) {
		String data;
		Bundle bundle = intent.getExtras();	
		
		/* The data payload of the message */
		data = (String) bundle.get("type");
		Log.d(LOG_TAG, "handleMessage, data=" + data);
		
		// generates a system notification to start a DM session
		Event ev = new Event("DMA_MSG_NET_NOTIFICATION");
		
		int cancelType = CANCEL_SESSION_NO_CANCEL;
		if (data.equals(SESSION_PUSH_LAWMO)) 
			cancelType = CANCEL_SESSION_PUSH_LAWMO;
		else if(data.equals(SESSION_PURGE_UPDATE))
			cancelType = CANCEL_SESSION_PURGE_UPDATE;
		
		ev.addVar(new EventVar("DMA_VAR_CANCEL_TYPE", cancelType));
		sendEvent(context, serviceClass, ev);
	}
	
	private Intent convertImplicitToExplisitIntent(Context ctx, Intent implicitIntent) {
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
 
        if (resolveInfo == null) {
        	Log.e(LOG_TAG, "resolveInfo is null");
        	return null;
        }
        Intent explicitIntent = null;
        if(resolveInfo.size() > 0) {
        	ResolveInfo info = resolveInfo.get(0);
        	String packageName = info.serviceInfo.packageName;
        	String className = info.serviceInfo.name;
        	ComponentName component = new ComponentName(packageName, className);

        	explicitIntent = new Intent(implicitIntent);
        	explicitIntent.setComponent(component);
        } else {
        	Log.e(LOG_TAG, "no GCM service installed on device!");
        }
        return explicitIntent;
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
		
		Intent intent = convertImplicitToExplisitIntent(ctx,registrationIntent);
		if (intent == null){
			sendGcmFailedEvent(ctx);
			Log.e(LOG_TAG, "registerToNotificationService: could not register GCM");
			return;
		}

		intent.putExtra("app",
			PendingIntent.getBroadcast(ctx, 0, new Intent(), 0));
		intent.putExtra("sender", senderId);
		ctx.startService(intent);
			
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
		Log.d(LOG_TAG, "sendNotifEvent, " + NOTIF_ID + "=" + inRegistrationId);

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
		sendGcmFailedEvent(ctx);

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
	
}
