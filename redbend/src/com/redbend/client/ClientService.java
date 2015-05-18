/*
 *******************************************************************************
 *
 * ClientService.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.*;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.redbend.app.*;
import com.redbend.client.ui.*;
import com.redbend.client.uialerts.*;
import com.redbend.client.R;
import com.redbend.client.SmmConstants;
import com.redbend.swm_common.ui.AdminUiBase;
import com.redbend.swm_common.ui.SetPasswordPolicyUi;
import com.redbend.swm_common.uialerts.*;
import com.redbend.swm_common.BookmarkHandler;

/**
 * Implements SmmService abstract class to handle events. Sends and receives
 * events, using intents to and from the BLL using BasicService. Registers
 * handlers for DIL events. In addition, has listeners for the device state
 * that generate BL events.
 */
public class ClientService extends SmmService
{
	/* UI ALERT TYPES */
	public final static int DMA_UI_ALERT_TYPE_INFO  = 1;
	public final static int DMA_UI_ALERT_TYPE_CONFIRMATION  = 2;
	public final static int DMA_UI_ALERT_TYPE_INPUT  = 3;
	
	private ConnectivityStateChangeReceiver stateChangeReceiver;

	public static enum PRODUCT_TYPE{
		SYSTEM,
		DOWNLOADABLE,
	}
	
	protected TelephonyManager m_telephonyManager;
	private boolean m_killProc = false;
	
	private void printClientVersion()
	{	
		String version;
		
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			version = "unknown";
		}
		Log.d(LOG_TAG, " **** Red Bend Software Client Version: " + version + " ****");
	}
	
	private void sendProductTypeEvent(){
		PRODUCT_TYPE pType = getProductType(this);
		Log.d(LOG_TAG, "sendProducEvent product type: " + pType.toString());
		Event product_type = new Event("DMA_MSG_PRODUCT_TYPE").addVar(new EventVar("DMA_VAR_PRODUCT_TYPE", pType.ordinal()));		
		sendEvent(product_type);
	}
	
	@Override
	public void onCreate() 
	{
		String []autoSelfRegDomainInfo = new String[2];

		Log.i(LOG_TAG, "onCreate");
		super.onCreate();
		/* define the receiver by explicit name */
		defineIntentReceiver("com.redbend.client", "BasicService");

		/* Get the telephony manager */
		m_telephonyManager = 
			(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);		
		printClientVersion();

		eventHandlersRegister();
		registerEventReceiver();
		initConnectivity();
		sendProductTypeEvent();
		setBackgroundNotification(1, R.drawable.notif_icon, getText(R.string.app_name), getText(R.string.notif_text));
		
		// set auto self registration nodes
		if (Ipl.iplGetAutoSelfRegDomainInfo(autoSelfRegDomainInfo) == 0)
		{
			sendEvent(new Event("DMA_MSG_AUTO_SELF_REG_INFO").addVar(new
				EventVar("DMA_VAR_AUTO_SELF_REG_DOMAIN_NAME", autoSelfRegDomainInfo[0])).addVar(new
				EventVar("DMA_VAR_AUTO_SELF_REG_DOMAIN_PIN", autoSelfRegDomainInfo[1])));
		}

		AdminUiBase.enableAdmin(this, (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE),
				true);
	}

	public static PRODUCT_TYPE getProductType(Context context) {
		return isPermissionGranted(Manifest.permission.INSTALL_PACKAGES, context) ? PRODUCT_TYPE.SYSTEM
				: PRODUCT_TYPE.DOWNLOADABLE;
	}
	
	/** method called from JNI on UI event receive */
	protected void recvEvent(byte ev[])
	{
		try {
			super.recvEvent(new Event(ev));
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error decoding received UI event");
		}
	}
    
	@Override
	public void sendEvent(Event ev) {
		// using sendIntentEvent in super
		sendIntentEvent(ev);
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "+onDestroy");
		super.onDestroy();
		// We need to kill the process to clean process memory
		if (m_killProc)
			android.os.Process.killProcess(android.os.Process.myPid());
		unregisterReceiver(stateChangeReceiver);		
	}	

	private void eventHandlersRegister()
	{
		Log.i(LOG_TAG, "+eventHandlersRegister");

		BatteryLowNotificationHandler requestChargeNotifyer = new BatteryLowNotificationHandler(this);		
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BACKGROUND, requestChargeNotifyer);
		
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)), 
				UI_MODE_FOREGROUND,
					new EventHandlerIntent(this, BatteryLow.class));

		EventHandlerIntent errorHandler = new EventHandlerIntent(this, ErrorHandler.class);
		registerHandler(1, new Event("DMA_MSG_DM_ERROR_UI"),
				UI_MODE_BOTH_FG_AND_BG,
				errorHandler);

		registerHandler(1, new Event("DMA_MSG_DL_INST_ERROR_UI"),
				UI_MODE_FOREGROUND,
				errorHandler);
		
		VsenseServerAttributeChangeHandler attributeChangeHandler = new VsenseServerAttributeChangeHandler(this);
		
		registerHandler(1, new Event("DMA_MSG_DM_VSENSE_SERVER_ATTR"),
				UI_MODE_BOTH_FG_AND_BG,
				attributeChangeHandler);
		
		registerHandler(1, new Event("DMA_MSG_DM_DOMAIN_NAME"),
				UI_MODE_BOTH_FG_AND_BG,
				attributeChangeHandler);

		registerHandler(1, new Event("DMA_MSG_DM_VSENSE_SERVER_ADDR"),
				UI_MODE_BOTH_FG_AND_BG,
				attributeChangeHandler);
		
		registerHandler(1, new Event("DMA_MSG_DM_VSENSE_SERVER_PORT"),
				UI_MODE_BOTH_FG_AND_BG,
				attributeChangeHandler);

		registerHandler(1, new Event("VSM_POLLING_INTERVAL_EVENT"),
				UI_MODE_BOTH_FG_AND_BG,
				attributeChangeHandler);

		EventHandler dlInterruptionNotifier = new InterruptionNotificiationHandler(this);

		//show scomo dl failure as _notification_
		registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
			.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
			.addVar(new EventVar("DMA_VAR_DL_RETRY_COUNTER", 0)),
				UI_MODE_BACKGROUND, dlInterruptionNotifier);
		
		//show scomo dl canceled as _notification_
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CANCELED_UI"),
				UI_MODE_BACKGROUND, dlInterruptionNotifier);		
		
		EventHandlerIntent scomoDlInterrupt = new EventHandlerIntent(this, 
				ScomoDownloadInterrupted.class);
		//show scomo dl canceled as _screen_
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CANCELED_UI"),
				UI_MODE_FOREGROUND,	scomoDlInterrupt );

		//show scomo dl failure as _screen_
		registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
			.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
			.addVar(new EventVar("DMA_VAR_DL_RETRY_COUNTER", 0)),
				UI_MODE_FOREGROUND, scomoDlInterrupt);

		//show scomo dl failure as _screen_
		registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
				.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
				.addVar(new EventVar("DMA_VAR_USER_INIT", 1)),
				UI_MODE_FOREGROUND,	scomoDlInterrupt );

		registerHandler(2, new Event("DMA_MSG_UI_ALERT"). 
				addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_CONFIRMATION)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, ConfirmUIAlert.class));

		registerHandler(2, new Event("DMA_MSG_UI_ALERT").
				addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INFO)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, InfoUIAlert.class));

		registerHandler(2, new Event("DMA_MSG_UI_ALERT").
				addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INPUT)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, InputUIAlert.class));

		// scomo user-initiated dl confirmation
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BOTH_FG_AND_BG, 
				new EventHandlerIntent(this, ScomoConfirm.class));

		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
				UI_MODE_BOTH_FG_AND_BG, 
				new EventHandler(this) {
					@Override
					protected void genericHandler(Event ev) {
						sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
					}
				});
		
		// scomo user-initiated dl suspend
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_FOREGROUND, 
				new EventHandlerIntent(this, ScomoDownloadSuspend.class));
		
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI_FROM_ICON").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BOTH_FG_AND_BG, 
				new EventHandlerIntent(this, ScomoDownloadSuspend.class));
		
		//In case user initiated and in silent - show error message "in progress"
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI")
			.addVar(new EventVar("DMA_VAR_SCOMO_MODE", SmmConstants.SCOMO_MODE_USER))
			.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),	UI_MODE_BOTH_FG_AND_BG, 
				new EventHandlerIntent(this, ErrorHandler.class));
		
		EventHandlerIntent checkUpdate =
				new EventHandlerIntent(this, LoadingActivity.class);
		/* user initiated application launching */
		registerHandler(1, new Event("DMA_MSG_USER_SESSION_TRIGGERED"),
				UI_MODE_FOREGROUND, checkUpdate);
		
		EventHandlerIntent scomoInstallConfirmHandler =
			new EventHandlerIntent(this, ScomoInstallConfirm.class);
		/* Non-silent, background or foreground, initiated by user. */
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
				.addVar(new EventVar("DMA_VAR_SCOMO_MODE", SmmConstants.SCOMO_MODE_USER)),
				UI_MODE_BOTH_FG_AND_BG, scomoInstallConfirmHandler);

		/* Non-silent, background or foreground, initiated by server. */
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
				.addVar(new EventVar("DMA_VAR_SCOMO_MODE", SmmConstants.SCOMO_MODE_SERVER)),
				UI_MODE_BOTH_FG_AND_BG, scomoInstallConfirmHandler);

		/* Silent, background or foreground, initiated by server, user, device. */
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
				UI_MODE_BOTH_FG_AND_BG, 
				new EventHandler(this) {
					@Override
					protected void genericHandler(Event ev) {
						sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
					}
				});
		
		/* Stop client service requested, this is not a BL event, it rather comes from BasicService */
		registerHandler(1, new Event(BasicService.STOP_CLIENT_EVENT),
				UI_MODE_BOTH_FG_AND_BG, 
				new EventHandler(this) {
					@Override
					protected void genericHandler(Event ev) {
						finishAllFlows(true);
						m_killProc = true; // kill the process to clean memory
						stopSelf();
					}
				});
		
		/* Non-silent, foreground, initiated by decive. */
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
			.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
			.addVar(new EventVar("DMA_VAR_SCOMO_MODE", SmmConstants.SCOMO_MODE_DEVICE)),
			UI_MODE_FOREGROUND, scomoInstallConfirmHandler);

		/* Non-silent, background, initiated by device. */
		registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
			.addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
			.addVar(new EventVar("DMA_VAR_SCOMO_MODE", SmmConstants.SCOMO_MODE_DEVICE)),
			UI_MODE_BACKGROUND,
			new InstallConfirmNotificationHandler(this));

		// show scomo download notification
		DownloadConfirmNotificationHandler dlSCOMONotifHandler = 
			new DownloadConfirmNotificationHandler(this);
		registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BACKGROUND, dlSCOMONotifHandler);
		registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
				UI_MODE_BACKGROUND,
				new EventHandler(this) {
					@Override
					protected void genericHandler(Event ev) {
						sendEvent(new Event("DMA_MSG_SCOMO_NOTIFY_DL"));
					}
				});
		
		// send broadcast event when DP is availble and installation end
		IntentBroadcaster intentBrodcaster = new IntentBroadcaster(this);
		registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BACKGROUND,
				intentBrodcaster);
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BOTH_FG_AND_BG, 
				intentBrodcaster);		
		registerHandler(1, new Event("DMA_MSG_SCOMO_FLOW_END_UI"),
				UI_MODE_BOTH_FG_AND_BG,
				intentBrodcaster);

		registerHandler(1, new Event("DMA_MSG_DL_INST_ERROR_UI"),
				UI_MODE_BACKGROUND, dlSCOMONotifHandler);
		
		// show scomo installation-progress as _screen_
		registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_PROGRESS_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, ScomoInstallProgress.class));		
		
		// show scomo installation-progress as _notification_
		ProgressNotificationHandler installNotifHandler = 
			new ProgressNotificationHandler(this);
		
		registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_PROGRESS_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BACKGROUND, installNotifHandler);

		// show scomo installation-done as _screen_
		registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_DONE").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, ScomoInstallDone.class));		
		
		// show scomo installation-done as _notification_
		InstallDoneNotificationHandler installDoneNotifHandler = 
			new InstallDoneNotificationHandler(this);
		registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_DONE"),
				UI_MODE_BACKGROUND, installDoneNotifHandler);
		
		// show scomo postpone-confirmation as _screen_
		registerHandler(1, new Event("DMA_MSG_SCOMO_POSTPONE_STATUS_UI"),
				UI_MODE_BOTH_FG_AND_BG,
				new EventHandlerIntent(this, ScomoPostponeConfirm.class));		
		
		// show scomo dl progress as _screen_
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_INIT"),
				UI_MODE_FOREGROUND,
				new StartDownload(this));

		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_PROGRESS").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, ScomoDownloadProgress.class));
		
		// show scomo dl progress as _notification_
		ProgressNotificationHandler dlProgreessNotifyer = new ProgressNotificationHandler(this);		
		registerHandler(1, new Event("DMA_MSG_SCOMO_DL_PROGRESS").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
				UI_MODE_BACKGROUND, dlProgreessNotifyer);

		registerHandler(1, new Event("DMA_MSG_SET_DL_COMPLETED_ICON"),
				UI_MODE_BOTH_FG_AND_BG, new DownloadCompleteNotificiationHandler(this));

		//SCOMO device-initiated
		registerHandler(1, new Event("DMA_MSG_SCOMO_SET_DL_TIMESLOT"),
				UI_MODE_BOTH_FG_AND_BG,
				new ScomoAlarmSetter(this));

		//LAWMO Wipe
		registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_AGENT_LAUNCH"),
				UI_MODE_BOTH_FG_AND_BG,
				new WipeAgentHandler(this));

		registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_SUCCESS"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoWipeResult.class));

		registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_FAILURE"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoWipeResult.class));

		registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_NOT_PERFORMED"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoWipeResult.class));

		//LAWMO Lock
		registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_LAUNCH"),
				UI_MODE_BOTH_FG_AND_BG,
				new LockingHandler(this));

		registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoLockResult.class));

		registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoLockResult.class));
		
		LawmoLockResultNotification lockNotifyer = new LawmoLockResultNotification(this);
		registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS"),
				UI_MODE_FOREGROUND,lockNotifyer);
		registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE"),
				UI_MODE_FOREGROUND, lockNotifyer);

		//LAWMO UnLock    	
		registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_LAUNCH"),
				UI_MODE_BOTH_FG_AND_BG,
				new UnLockHandler(this));

		registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoUnLockResult.class));

		registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE"),
				UI_MODE_FOREGROUND,
				new EventHandlerIntent(this, LawmoUnLockResult.class));

		LawmoUnLockResultNotification unLockNotifyer = new LawmoUnLockResultNotification(this);
		registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS"),
				UI_MODE_FOREGROUND,unLockNotifyer);
		registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE"),
				UI_MODE_FOREGROUND, unLockNotifyer);

		EventHandlerIntent passwordPolicyHandler =
			new EventHandlerIntent(this, SetPasswordPolicyUi.class);
		registerHandler(1, new Event("MSG_DESCMO_SET_PASSWORD"),
				UI_MODE_BOTH_FG_AND_BG, passwordPolicyHandler);
		registerHandler(1, new Event("MSG_DESCMO_USER_INTERACTION_TIMEOUT"),
				UI_MODE_BOTH_FG_AND_BG, passwordPolicyHandler);
		DescmoNotificationHandler descmoNotifyer =
			new DescmoNotificationHandler(this);
		registerHandler(1, new Event("MSG_DESCMO_SET_PASSWORD"),
				UI_MODE_BACKGROUND, descmoNotifyer);

		registerHandler(1, new Event("DMA_MSG_SCOMO_REBOOT_REQUEST"), 
				UI_MODE_BOTH_FG_AND_BG,
				new EventHandlerIntent(this, RecoveryReboot.class));
		
	    registerHandler(1, new Event("DMA_MSG_GCM_REGISTRATION_DATA"),
				UI_MODE_BOTH_FG_AND_BG, new RBGcmHandler(this));
	    
	    registerHandler(1, new Event("DMA_MSG_GCM_UN_REGISTRATION_DATA"),
				UI_MODE_BOTH_FG_AND_BG, new RBGcmHandler(this));
				
	    registerHandler(1, new Event("DMA_MSG_GET_BATTERY_LEVEL"),
				UI_MODE_BOTH_FG_AND_BG, new GetBatteryLevelHandler(this));
	    
	    EventHandlerIntent installComp = new EventHandlerIntent(this, InstallApk.class);

	    registerHandler(3, new Event("DMA_MSG_SCOMO_INSTALL_COMP_REQUEST"),
				UI_MODE_BOTH_FG_AND_BG, installComp);

	    registerHandler(3, new Event("DMA_MSG_SCOMO_REMOVE_COMP_REQUEST"),
				UI_MODE_BOTH_FG_AND_BG, installComp);

	    registerHandler(3, new Event("DMA_MSG_SCOMO_CANCEL_COMP_REQUEST"),
				UI_MODE_BOTH_FG_AND_BG, installComp);
	    
	    registerHandler(3, new Event("DMA_MSG_SCOMO_INSTALL_DONE"),
	    		UI_MODE_BOTH_FG_AND_BG, installComp);
	    
	     // enroll bookmark
		EventHandler bookmarkHandler = new BookmarkHandler(this);
		registerHandler(1, new Event("DMA_MSG_ENROLL_PUT_BOOKMARK"),
				UI_MODE_BOTH_FG_AND_BG, bookmarkHandler);
		registerHandler(1, new Event("DMA_MSG_ENROLL_REMOVE_BOOKMARK"),
				UI_MODE_BOTH_FG_AND_BG, bookmarkHandler);
	}
	
	private class DilPhoneStateListener extends PhoneStateListener {
		private final static boolean ROAMING_INITIAL_VALUE = true;
		private final static boolean CALL_INITIAL_VALUE = false;

		private boolean inRoaming = ROAMING_INITIAL_VALUE;
		private boolean inCall = CALL_INITIAL_VALUE;

		private synchronized void sendRoamingUpdate(boolean roaming)
		{
			if (roaming == inRoaming)
				return;

			Event event = new Event("DMA_MSG_STS_ROAMING");
			event.addVar(new EventVar("DMA_VAR_STS_IS_ROAMING", roaming ? 1 : 0 ));
			sendEvent(event);
			inRoaming = roaming;
		}

		private synchronized void sendVoiceCallUpdate(boolean call)
		{
			if (call == inCall)
				return;

			String eventId = call ? "DMA_MSG_STS_VOICE_CALL_START" : "DMA_MSG_STS_VOICE_CALL_STOP";
			Log.d(LOG_TAG, "Sending voice call Update, new state: " + call);
			sendEvent(new Event(eventId));
			inCall = call;
		}

		@Override
		public void onCallStateChanged(int state,String incomingNumber) {

			switch(state)
			{               
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d(LOG_TAG, "Call state OFFHOOK, in call");
				sendVoiceCallUpdate(true);
				break;
			case TelephonyManager.CALL_STATE_RINGING:		            
			case TelephonyManager.CALL_STATE_IDLE:
			default:
				sendVoiceCallUpdate(false);
				Log.d(LOG_TAG, "Call state IDLE, no call");
				break;
			}
		}

		public void updateRoamingState(boolean isRoaming) {
			Log.d(LOG_TAG, "State Change,  Roaming: " + isRoaming); 
			sendRoamingUpdate(isRoaming);
		}

		@Override	
		public void onServiceStateChanged(ServiceState serviceState)
		{
			updateRoamingState(serviceState.getRoaming());

		}
	}
	
	private void sendInitialRoamingState(ConnectivityManager connManager, DilPhoneStateListener listener)
	{
		NetworkInfo info = connManager.getActiveNetworkInfo();
		
		Log.i(LOG_TAG, "Sending initial Roaming state");
		if (info == null || !info.isConnected())
			listener.updateRoamingState(false);
		else
			listener.updateRoamingState(info.isRoaming());
	}

	private void initConnectivity() {
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (connManager == null) {
			Log.e(LOG_TAG, "No ConnectivityManager found!");
			return;
		}
		stateChangeReceiver = new ConnectivityStateChangeReceiver(false, false);
		DilPhoneStateListener listener = new DilPhoneStateListener();
		
		/* register state change receiver */
		registerReceiver(stateChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

		/* send initial update */
		NetworkInfo netInfo = connManager.getActiveNetworkInfo();
		if (netInfo != null)
			stateChangeReceiver.sendUpdate(this, netInfo);
		
		// register the listener with the telephony manager
		m_telephonyManager.listen(listener, 
				PhoneStateListener.LISTEN_SERVICE_STATE | PhoneStateListener.LISTEN_CALL_STATE);
		
		sendInitialRoamingState(connManager, listener);
	}
}
