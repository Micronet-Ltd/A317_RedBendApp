/*
 *******************************************************************************
 *
 * ClientService.java
 *
 * Implements SmmService abstract class to handle events. Sends and receives
 * events, using intents to and from the BLL using BasicService. Registers
 * handlers for DIL events. In addition, has listeners for the device state that
 * generate BL events.
 *
 * Sends BL events:
 * DMA_MSG_PRODUCT_TYPE
 * DMA_MSG_AUTO_SELF_REG_INFO
 * DMA_MSG_STS_ROAMING
 * DMA_MSG_STS_VOICE_CALL_START
 * DMA_MSG_STS_VOICE_CALL_STOP
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.*;
import java.util.Collection;
import java.util.TreeSet;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;

import com.redbend.android.RbException.VdmError;
import com.redbend.app.*;
import com.redbend.client.micronet.MicronetAppBroadcaster;
import com.redbend.client.micronet.MicronetConfirmHandler;
import com.redbend.client.ui.*;
import com.redbend.client.uialerts.*;
import com.redbend.client.R;
import com.redbend.client.AdminRequestActivity;
import com.redbend.swm_common.ui.SetEncryptionPolicyUi;
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

    private TreeSet<String> m_externalyzeEventsList = new TreeSet<String>();

    private ConnectivityStateChangeReceiver stateChangeReceiver;


    public final static int DMA_UI_ALERT_TYPE_INFO = 1;
    public final static int DMA_UI_ALERT_TYPE_CONFIRMATION = 2;
    public final static int DMA_UI_ALERT_TYPE_INPUT = 3;

    public final static String EVENT_NAME_FILTER = "event_name";
    public final static String SWMC_DIL_EVENT_ACTION  = "SwmcClient.SwmcDilEvent";
    public final static String COM_REDBEND_CLIENT_CATEGORY = "com.redbend.client";
    public static final String ANDROID_PERMISSION_ADMIN = "android.permission.BIND_DEVICE_ADMIN";


    public static enum PRODUCT_TYPE{
        SYSTEM,
        DOWNLOADABLE_TRUE,
        DOWNLOADABLE
    }

    protected TelephonyManager m_telephonyManager;
    private boolean m_killProc = false;

    private BroadcastReceiver m_rbAnalyticsManagerReciever = null;
    private static final String RB_ANALYTICS_INTENT_FILTER = "SwmClient.RB_ANALYTICS_STATE";
    private static final String RB_ANALYTICS_ENABLE_EXTRA_DATA = "enable_analytics";
    //Managed Space fix
    public static final String CLEAR_DATA_EVENT = "DMA_MSG_USER_CLEAR_DATA";
    private boolean m_duringManageSpace = false;

    private void printClientVersion()
    {	
        String version;

        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "unknown";
        }
        Log.d(LOG_TAG, " **** Red Bend Software Client Version: " + version + " ****");

        MicronetAppBroadcaster.sendStartingBroadcast(this);
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

        Log.i(LOG_TAG, "+onCreate");
        super.onCreate();

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
            UserHandle uh = android.os.Process.myUserHandle();
            Context context = getApplicationContext();
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            long userSerialNumber = 0;
            if (null != um) {
                userSerialNumber = um.getSerialNumberForUser(uh);
                if (userSerialNumber != 0) {
                    Log.d(LOG_TAG, "Only the primary user can run Software Management");
                    stopSelf();
                    return;
                }
            }
        } // >= Jelly Bean

        /* define the receiver by explicit name */
        defineIntentReceiver(COM_REDBEND_CLIENT_CATEGORY, "BasicService", getPackageName());		
        initExternalyzeList();

        m_duringManageSpace = false; //Managed Space fix

        /* Get the telephony manager */
        m_telephonyManager = 
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);		
        printClientVersion();

        eventHandlersRegister();
        registerEventReceiver();
        initConnectivity();
        sendProductTypeEvent();

        setBackgroundNotification(1, R.drawable.ic_notify_rb, getText(R.string.app_name), getText(R.string.notif_text));

        // set auto self registration nodes
        if (Ipl.iplGetAutoSelfRegDomainInfo(autoSelfRegDomainInfo) == 0){
            sendEvent(new Event("DMA_MSG_AUTO_SELF_REG_INFO").addVar(new
                    EventVar("DMA_VAR_AUTO_SELF_REG_DOMAIN_NAME", autoSelfRegDomainInfo[0])).addVar(new
                            EventVar("DMA_VAR_AUTO_SELF_REG_DOMAIN_PIN", autoSelfRegDomainInfo[1])));
        }
        
        Intent intent = new Intent(this, AdminRequestActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        //start RB Analytics Manager and set Analytics state in case this delivery includes Analytics
        if (RbAnalyticsHelper.isRbAnalyticsDelivery(getApplicationContext()))
        {
            //check and set RB Analytics
            boolean analyticsState = RbAnalyticsHelper.isRbAnaliticsRunning(getApplicationContext());
            RbAnalyticsHelper.setRbAnalyticsServiceState(getApplicationContext(),analyticsState);

            //init RB Analytics Manager
            initRbAnalyticsManagerReciever();
        }
    }

    /*
     * This method implements the API that allowing the OEM to start/stop 
     * RB Analytics service with broadcast intent, outside the SWMC application. 
     */
    private void initRbAnalyticsManagerReciever(){
        m_rbAnalyticsManagerReciever = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(LOG_TAG, "RB Analytics reciever: action="+action);
                if (action.equals(RB_ANALYTICS_INTENT_FILTER)){
                    boolean enableAnalytics = intent.getBooleanExtra(RB_ANALYTICS_ENABLE_EXTRA_DATA, true);
                    RbAnalyticsHelper.setRbAnalyticsServiceState(getApplicationContext(), enableAnalytics);
                }
            }
        };
        registerReceiver(m_rbAnalyticsManagerReciever, new IntentFilter(RB_ANALYTICS_INTENT_FILTER), 
                ANDROID_PERMISSION_ADMIN, null);
    }

    public static boolean isSystemApplication(Context inContext){
        Log.i("ClientService", "+isSystemApplication");
        PackageManager pm = inContext.getPackageManager();		
        ApplicationInfo appInfo;

        try {
            appInfo = pm.getApplicationInfo(inContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            appInfo = null;
        }
        boolean isSystemApp = false;
        if(appInfo != null){
            Log.i("ClientService", "isSystemApplication::appInfo != null");
            isSystemApp = ((appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0);
        }
        Log.i("ClientService", "-isSystemApplication::isSystemApp = " + isSystemApp);
        return isSystemApp;		
    }

    public static PRODUCT_TYPE getProductType(Context context) {
        boolean isSystem = ClientService.isSystemApplication(context);
        if(isSystem)
            return PRODUCT_TYPE.SYSTEM;

        return  context.getResources().getBoolean(R.bool.isTRUEProduct) ? PRODUCT_TYPE.DOWNLOADABLE_TRUE: PRODUCT_TYPE.DOWNLOADABLE;
    }

    @Override
    /** method called on UI event receive */
    protected void recvEvent(Event ev) {
        Log.d(LOG_TAG, "Received event " + ev.getName());
        if (isEventFromExternalyzeList(ev.getName()) == false) {
            super.recvEvent(ev);
        } else {
            sendIntentForOutsideEvent(ev);
        }
    }

    /** method called from JNI on UI event receive */
    protected void recvEvent(byte ev[])	{
        Event event = null;
        try {
            event = new Event(ev);
            if (isEventFromExternalyzeList(event.getName()) == false) {
                super.recvEvent(event);
            } else {
                sendIntentForOutsideEvent(event);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void sendEvent(Event ev) {
        // using sendIntentEvent in super

        //Managed Space fix
        String eventName = ev.getName();
        if (CLEAR_DATA_EVENT.equals(eventName) )
        {
            if (m_duringManageSpace)
            {
                Log.e(LOG_TAG, "CLEAR_DATA_EVENT ignored! probebly during duplicate Manage Space");
                return;
            }
            m_duringManageSpace = true;
        }

        sendIntentEvent(ev);
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "+onDestroy");
        super.onDestroy();
        // We need to kill the process to clean process memory
        if (m_killProc)
            android.os.Process.killProcess(android.os.Process.myPid());
        if (stateChangeReceiver != null)
            unregisterReceiver(stateChangeReceiver);	
        if (m_rbAnalyticsManagerReciever != null)
            unregisterReceiver(m_rbAnalyticsManagerReciever);

        m_duringManageSpace = false; //Managed Space fix

    }

    private void initExternalyzeList() {
        Log.d(LOG_TAG, "+initExternalyzeList()");
        Resources r = getResources();

        int id = r.getIdentifier("eventsForExternalize", "array", getPackageName());
        Log.d(LOG_TAG, "initExternalyzeList()=>checkExistence of array = " + id);
        if (id != 0) {
            for (String event : r.getStringArray(id))
                m_externalyzeEventsList.add(event);
        }
        Log.d(LOG_TAG, "-initExternalyzeList()");
    }

    private boolean isEventFromExternalyzeList(String inEventName){
        Log.i(LOG_TAG, "+isEventFromExternalyzeList=>inEventName = " + inEventName);
        boolean result = m_externalyzeEventsList.contains(inEventName);
        Log.i(LOG_TAG, "-isEventFromExternalyzeList=>result = " + result);
        return result;
    }


    private void sendIntentForOutsideEvent(Event ev) {
        Log.d(LOG_TAG,
                "+sendIntentForOutsideEvent=>event name: " + ev.getName());
        Intent outsideEventIntent = new Intent(SWMC_DIL_EVENT_ACTION);
        outsideEventIntent.addCategory(COM_REDBEND_CLIENT_CATEGORY);		
        outsideEventIntent.putExtra(EVENT_NAME_FILTER, ev.getName());
        Collection<EventVar> vars = ev.getVars();
        for (EventVar var : vars) {
            var.addToIntent(outsideEventIntent);
        }
        sendBroadcast(outsideEventIntent);
        Log.d(LOG_TAG, "-sendIntentForOutsideEvent");
    }


    private void Html5EventRegister()
    {
        //If user launched the application - show HTML5
        registerHandler(1, new Event("B2D_APPLICATION_START_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)), 
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_DL_INST_ERROR_UI"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_DM_ERROR_UI"),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
        .addVar(new EventVar("DMA_VAR_DL_RETRY_COUNTER", 0)),
        UI_MODE_FOREGROUND, 
        new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
        .addVar(new EventVar("DMA_VAR_ERROR", VdmError.BAD_DD_INVALID_SIZE.val)),
        UI_MODE_FOREGROUND, 
        new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
        .addVar(new EventVar("DMA_VAR_ERROR", VdmError.PURGE_UPDATE.val)),
        UI_MODE_FOREGROUND, 
        new EventHandlerIntent(this, Html5Container.class));

        //show scomo dl failure as _screen_
        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
        .addVar(new EventVar("DMA_VAR_USER_INIT", 1)),
        UI_MODE_FOREGROUND,	
        new EventHandlerIntent(this, Html5Container.class));

//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, Html5Container.class));

        //In case user initiated and in silent - show error message "in progress"
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_TRIGGER_MODE", SmmConstants.SCOMO_MODE_USER))
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),	UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this, Html5Container.class));

        // user initiated application launching 
        registerHandler(1, new Event("DMA_MSG_USER_SESSION_TRIGGERED"),
                UI_MODE_FOREGROUND, 
                new EventHandlerIntent(this, Html5Container.class));

        //send accept in case condition are met
        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_UI_CONDITIONS").addVar(new EventVar("DMA_VAR_CONDITIONS_MET", 1)),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
            }
        });	
        /* add conditions event to show UI */
        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_UI_CONDITIONS").addVar(new EventVar("DMA_VAR_CONDITIONS_MET", 0)),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandlerIntent(this, Html5Container.class));

        //**************Event DMA_MSG_SCOMO_INS_CONFIRM_UI start********************

        // Non-silent, critical, background or foreground, initiated by user, server, device. 
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_SCOMO_CRITICAL", 1)),
//        UI_MODE_BOTH_FG_AND_BG,
//        new EventHandlerIntent(this, Html5Container.class));
//
//        // Non-silent, Non-critical, foreground, initiated by user, device, server.
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_SCOMO_CRITICAL", 0)),
//        UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this, Html5Container.class));

        //**************Event DMA_MSG_SCOMO_INS_CONFIRM_UI end********************

        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_PROGRESS_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));		

        // show scomo installation-done as _screen_
        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_DONE").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));	

        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_PROGRESS").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));	

        registerHandler(1, new Event("DMA_MSG_SCOMO_REBOOT_CONFIRM_REQUEST"), 
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, Html5Container.class));

        // user initiated application launching 
        registerHandler(1, new Event("DMA_MSG_USER_SESSION_TRIGGERED"),
                UI_MODE_FOREGROUND, 
                new EventHandlerIntent(this, Html5Container.class));

        // show scomo postpone-confirmation as _screen_
        registerHandler(1, new Event("DMA_MSG_SCOMO_POSTPONE_STATUS_UI"),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, Html5Container.class));	

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_NOT_PERFORMED"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("B2D_HTTP_COMMAND_RESPONSE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_UI_ALERT"). 
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_CONFIRMATION)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_UI_ALERT").
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INFO)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_UI_ALERT").
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INPUT)),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));		

        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, Html5Container.class));

        // TODO: add new html page for suspend ui
        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_FOREGROUND, 
                new EventHandlerIntent(this, ScomoDownloadSuspend.class));

        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI_FROM_ICON").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandlerIntent(this, ScomoDownloadSuspend.class));
    }

    private void AndroidEventRegister()
    {

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoWipeResult.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoWipeResult.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_RESULT_NOT_PERFORMED"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoWipeResult.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoLockResult.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoLockResult.class));

        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoLockResult.class));		

        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,
                new EventHandlerIntent(this, LawmoLockResult.class));

//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, BatteryLow.class));

//        registerHandler(1, new Event("DMA_MSG_DL_INST_ERROR_UI"),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, ErrorHandler.class));
//
//        registerHandler(1, new Event("DMA_MSG_DM_ERROR_UI"),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, ErrorHandler.class));

//        EventHandlerIntent scomoDlInterrupt = new EventHandlerIntent(this,
//                ScomoDownloadInterrupted.class);
//        //show scomo dl canceled as _screen_
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CANCELED_UI"),
//                UI_MODE_FOREGROUND,	scomoDlInterrupt );

//        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_DL_RETRY_COUNTER", 0)),
//        UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this,
//                ScomoDownloadInterrupted.class));

//        //show scomo dl failure as _screen_
//        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_USER_INIT", 1)),
//        UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this,
//                ScomoDownloadInterrupted.class));
//
//        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_ERROR", VdmError.BAD_DD_INVALID_SIZE.val)),
//        UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this, ScomoDownloadInterrupted.class));

//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, ScomoConfirm.class));

        //In case user initiated and in silent - show error message "in progress"
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_TRIGGER_MODE", SmmConstants.SCOMO_MODE_USER))
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),	UI_MODE_FOREGROUND,
//        new EventHandlerIntent(this, ErrorHandler.class));

//        EventHandlerIntent checkUpdate =
//                new EventHandlerIntent(this, LoadingActivity.class);
//        // user initiated application launching
//        registerHandler(1, new Event("DMA_MSG_USER_SESSION_TRIGGERED"),
//                UI_MODE_BOTH_FG_AND_BG, checkUpdate);

//        // Non-silent, background or foreground, initiated by user.
//        EventHandlerIntent scomoInstallConfirmHandler =
//                new EventHandlerIntent(this, ScomoInstallConfirm.class);

//        // Non-silent, critical, background or foreground, initiated by user, server, device.
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_SCOMO_CRITICAL", 1)),
//        UI_MODE_BOTH_FG_AND_BG, scomoInstallConfirmHandler);
//
//        // Non-silent, Non-critical, foreground, initiated by user, device, server.
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_SCOMO_CRITICAL", 0)),
//        UI_MODE_FOREGROUND, scomoInstallConfirmHandler);

//        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_PROGRESS_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, ScomoInstallProgress.class));

        // show scomo installation-done as _screen_
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_DONE").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, ScomoInstallDone.class));

//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_PROGRESS").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, ScomoDownloadProgress.class));

//        registerHandler(1, new Event("DMA_MSG_SCOMO_REBOOT_CONFIRM_REQUEST"),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, RequestRebootConfirm.class));

//        // show scomo postpone-confirmation as _screen_
//        registerHandler(1, new Event("DMA_MSG_SCOMO_POSTPONE_STATUS_UI"),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, ScomoPostponeConfirm.class));

        // UI Alert registration for Android
        registerHandler(2, new Event("DMA_MSG_UI_ALERT"). 
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_CONFIRMATION)),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, ConfirmUIAlert.class));

        registerHandler(2, new Event("DMA_MSG_UI_ALERT").
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INFO)),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, InfoUIAlert.class));

        registerHandler(2, new Event("DMA_MSG_UI_ALERT").
                addVar(new EventVar("DMA_VAR_UI_ALERT_TYPE", DMA_UI_ALERT_TYPE_INPUT)),
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, InputUIAlert.class));

        // scomo user-initiated dl suspend
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_FOREGROUND,
//                new EventHandlerIntent(this, ScomoDownloadSuspend.class));
//
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_SUSPEND_UI_FROM_ICON").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandlerIntent(this, ScomoDownloadSuspend.class));
    }

    private void eventHandlersRegister()
    {
        Log.i(LOG_TAG, "+eventHandlersRegister");

        Resources res = getResources();
        boolean isAutomotive= res.getBoolean(R.bool.isAutomotive);
        Log.i(LOG_TAG, "isAutomotive:" + isAutomotive);

        EventHandler dlInterruptionNotifier = new InterruptionNotificiationHandler(this);
        BatteryLowNotificationHandler requestChargeNotifyer = new BatteryLowNotificationHandler(this);
        VsenseServerAttributeChangeHandler attributeChangeHandler = new VsenseServerAttributeChangeHandler(this);

//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CHARGE_BATTERY_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BACKGROUND, requestChargeNotifyer);

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

        registerHandler(1, new Event("DMA_MSG_DM_VSENSE_POLLING_INTERVAL"),
                UI_MODE_BOTH_FG_AND_BG,
                attributeChangeHandler);

        //show scomo dl canceled as _notification_
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CANCELED_UI"),
//                UI_MODE_BACKGROUND, dlInterruptionNotifier);

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


        // show scomo download notification
//        DownloadConfirmNotificationHandler dlSCOMONotifHandler =
//                new DownloadConfirmNotificationHandler(this);
//        registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BACKGROUND, dlSCOMONotifHandler);
//        registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
//                UI_MODE_BACKGROUND,
//                new EventHandler(this) {
//            @Override
//            protected void genericHandler(Event ev) {
//                Log.d(LOG_TAG, "sending DMA_MSG_SCOMO_NOTIFY_DL from silent");
//                sendEvent(new Event("DMA_MSG_SCOMO_NOTIFY_DL"));
//            }
//        });

        // send broadcast event when DP is availble and installation end
        IntentBroadcaster intentBrodcaster = new IntentBroadcaster(this);
        registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
                UI_MODE_BACKGROUND,
                intentBrodcaster);

        // In case got DMA_MSG_SCOMO_NOTIFY_DL_UI and in foreground
        // send DMA_MSG_SCOMO_NOTIFY_DL back to the engine.
        // The flag UI_MODE_FOREGROUND in the registerHandler has no 
        // affect on event handlers, only on activities.
//        final int flowId = 1;
//        registerHandler(flowId, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI"),
//                UI_MODE_FOREGROUND,//no real affect when using genericHandler
//                new EventHandler(this) {
//            @Override
//            protected void genericHandler(Event ev) {
//                Log.d(LOG_TAG,
//                        "sending DMA_MSG_SCOMO_NOTIFY_DL from foreground");
//                if (isFlowInForeground(flowId))
//                    sendEvent(new Event("DMA_MSG_SCOMO_NOTIFY_DL"));
//            }
//        });

        registerHandler(1, new Event("DMA_MSG_SCOMO_FLOW_END_UI"),
                UI_MODE_BOTH_FG_AND_BG,
                intentBrodcaster);

//        registerHandler(1, new Event("DMA_MSG_DL_INST_ERROR_UI"),
//                UI_MODE_BACKGROUND, dlInterruptionNotifier);

        // show scomo installation-progress as _notification_
//        ProgressNotificationHandler installNotifHandler =
//                new ProgressNotificationHandler(this);

//        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_PROGRESS_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BACKGROUND, installNotifHandler);

        // show scomo installation-done as _notification_
//        InstallDoneNotificationHandler installDoneNotifHandler =
//                new InstallDoneNotificationHandler(this);
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INSTALL_DONE"),
//                UI_MODE_BACKGROUND, installDoneNotifHandler);

        // show scomo dl progress as _screen_
        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_INIT"),
                UI_MODE_FOREGROUND,
                new StartDownload(this));

//        // show scomo dl progress as _notification_
//        ProgressNotificationHandler dlProgreessNotifyer = new ProgressNotificationHandler(this);
//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_PROGRESS").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BACKGROUND, dlProgreessNotifyer);

//        // show dl complete notification only if we are not in silent mode
//        registerHandler(1, new Event("DMA_MSG_SET_DL_COMPLETED_ICON").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0)),
//                UI_MODE_BOTH_FG_AND_BG, new DownloadCompleteNotificiationHandler(this));

//        //SCOMO device-initiated
//        registerHandler(1, new Event("DMA_MSG_SCOMO_SET_DL_TIMESLOT"),
//                UI_MODE_BOTH_FG_AND_BG,
//                new ScomoAlarmSetter(this));

        //LAWMO Wipe
        registerHandler(1, new Event("DMA_MSG_LAWMO_WIPE_AGENT_LAUNCH"),
                UI_MODE_BOTH_FG_AND_BG,
                new WipeAgentHandler(this));

        //LAWMO Lock
        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_LAUNCH"),
                UI_MODE_BOTH_FG_AND_BG,
                new LockingHandler(this));

        registerHandler(1, new Event("B2D_LISTEN_TO_LOGIN_EVENT"),
                UI_MODE_BOTH_FG_AND_BG,
                new UserLoginRegistration(this));

        LawmoLockResultNotification lockNotifyer = new LawmoLockResultNotification(this);
        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,lockNotifyer);
        registerHandler(1, new Event("DMA_MSG_LAWMO_LOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND, lockNotifyer);

        //LAWMO UnLock    	
        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_LAUNCH"),
                UI_MODE_BOTH_FG_AND_BG,
                new UnLockHandler(this));

        LawmoUnLockResultNotification unLockNotifyer = new LawmoUnLockResultNotification(this);
        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_SUCCESS"),
                UI_MODE_FOREGROUND,unLockNotifyer);
        registerHandler(1, new Event("DMA_MSG_LAWMO_UNLOCK_RESULT_FAILURE"),
                UI_MODE_FOREGROUND, unLockNotifyer);

        EventHandlerIntent passwordPolicyHandler =
                new EventHandlerIntent(this, SetPasswordPolicyUi.class);
        registerHandler(1, new Event("MSG_DESCMO_SET_FEATURE").addVar(new EventVar("VAR_DESCMO_FEATURE_NAME","Password")),
                UI_MODE_BOTH_FG_AND_BG, passwordPolicyHandler);
        registerHandler(1, new Event("MSG_DESCMO_USER_INTERACTION_TIMEOUT"),
                UI_MODE_BOTH_FG_AND_BG, passwordPolicyHandler);
        DescmoNotificationHandler descmoNotifyer =
                new DescmoNotificationHandler(this);
        registerHandler(1, new Event("MSG_DESCMO_SET_FEATURE").addVar(new EventVar("VAR_DESCMO_FEATURE_NAME","Password")),
                UI_MODE_BACKGROUND, descmoNotifyer);

        EventHandlerIntent encryptionPolicyHandler =
                new EventHandlerIntent(this, SetEncryptionPolicyUi.class);
        registerHandler(1, new Event("MSG_DESCMO_SET_FEATURE").addVar(new EventVar("VAR_DESCMO_FEATURE_NAME","Encryption")),
                UI_MODE_BOTH_FG_AND_BG, encryptionPolicyHandler);
        registerHandler(1, new Event("MSG_DESCMO_GET_FEATURE_STATUS").addVar(new EventVar("VAR_DESCMO_FEATURE_NAME","Encryption")),
                UI_MODE_BOTH_FG_AND_BG, encryptionPolicyHandler);
        registerHandler(1, new Event("MSG_DESCMO_USER_INTERACTION_TIMEOUT"),
                UI_MODE_BOTH_FG_AND_BG, encryptionPolicyHandler);
        registerHandler(1, new Event("MSG_DESCMO_SET_FEATURE").addVar(new EventVar("VAR_DESCMO_FEATURE_NAME","Encryption")),
                UI_MODE_BACKGROUND, descmoNotifyer);

        registerHandler(1, new Event("DMA_MSG_SCOMO_REBOOT_REQUEST"), 
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, RequestReboot.class));

        registerHandler(1, new Event("DMA_MSG_GCM_REGISTRATION_DATA"),
                UI_MODE_BOTH_FG_AND_BG, new RBGcmHandler(this));

        registerHandler(1, new Event("DMA_MSG_GCM_UN_REGISTRATION_DATA"),
                UI_MODE_BOTH_FG_AND_BG, new RBGcmHandler(this));

        registerHandler(1, new Event("DMA_MSG_GET_BATTERY_LEVEL"),
                UI_MODE_BOTH_FG_AND_BG, new GetBatteryLevelHandler(this));

        EventHandler installComp ;
        PackageManager pm = getPackageManager();
        boolean isInstallPermissionGranted = pm.checkPermission(
                Manifest.permission.INSTALL_PACKAGES, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        if (isInstallPermissionGranted)
            installComp = new InstallApk(this);
        else
            installComp = new EventHandlerIntent(this, InstallApkNonRoot.class);

        registerHandler(3, new Event("DMA_MSG_SCOMO_INSTALL_COMP_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG, installComp);

        registerHandler(3, new Event("DMA_MSG_SCOMO_REMOVE_COMP_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG, installComp);

        registerHandler(3, new Event("DMA_MSG_SCOMO_CANCEL_COMP_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG, installComp);

        registerHandler(3, new Event("DMA_MSG_SCOMO_INSTALL_DONE"),
                UI_MODE_BOTH_FG_AND_BG, installComp);

        // Generic installer handle events
        registerHandler(1, new Event("B2D_MSG_SCOMO_GENERIC_INSTALL_REQUEST"), 
                UI_MODE_BOTH_FG_AND_BG,
                new GenericInstallerHandler(this));


        // enroll bookmark
        EventHandler bookmarkHandler = new BookmarkHandler(this);
        registerHandler(1, new Event("DMA_MSG_ENROLL_PUT_BOOKMARK"),
                UI_MODE_BOTH_FG_AND_BG, bookmarkHandler);
        registerHandler(1, new Event("DMA_MSG_ENROLL_REMOVE_BOOKMARK"),
                UI_MODE_BOTH_FG_AND_BG, bookmarkHandler);

//        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
//                UI_MODE_BACKGROUND,
//                new EventHandler(this) {
//            @Override
//            protected void genericHandler(Event ev) {
//                sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
//            }
//        });

//        //show scomo dl failure as _notification_
//        registerHandler(1, new Event("DMA_MSG_DNLD_FAILURE")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_DL_RETRY_COUNTER", 0)),
//        UI_MODE_BACKGROUND, dlInterruptionNotifier);

        // Register B2D_GET_ALL_CONDITIONS_VALUES only for reference code - this is a mock 
        registerHandler(1, new Event("B2D_GET_ALL_CONDITIONS_VALUES"),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                Log.d(LOG_TAG, "genericHandler got B2D_GET_ALL_CONDITIONS_VALUES");
                sendEvent(new Event("D2B_CONDITION_VALUE_UPDATE")
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_NAME","DIL_MocParameter1"))
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_VALUE",43)));

                sendEvent(new Event("D2B_CONDITION_VALUE_UPDATE")
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_NAME","DIL_MocParameter2"))
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_VALUE",70)));

                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                int bluetoothCondition = 0;

                if (mBluetoothAdapter != null)
                    bluetoothCondition = mBluetoothAdapter.isEnabled() ? 1 : 0;

                sendEvent(new Event("D2B_CONDITION_VALUE_UPDATE")
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_NAME","DIL_btCondParam"))
                .addVar(new EventVar("DMA_VAR_CONDITION_VAR_VALUE",bluetoothCondition)));
            }
        });

        registerHandler(1, new Event("B2D_BUFFER_READY"),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                File filesDir = getFilesDir();
                File dp = new File(filesDir.getAbsolutePath(), "RB_DP");
                Log.d(LOG_TAG, "dp length():" + dp.length());
                sendEvent(new Event("D2B_BUFFER_TRANSMITTED")
                .addVar(new EventVar("DMA_VAR_TX_BYTES", (int)dp.length())));

                byte[] dpContent = readFile(dp);
                File dpAppend = new File(filesDir.getAbsolutePath(), "RB_DP_APPEND");
                writeFile(dpContent, dpAppend);
                Log.d(LOG_TAG,"dpAppend length:" +dpAppend.length() );
            }
        });

        registerHandler(1, new Event("B2D_GET_INSTALL_TYPE"),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                sendEvent(new Event("D2B_SET_INSTALL_TYPE")
                .addVar(new EventVar("DMA_VAR_INSTALLTYPE", 0)));
            }
        });

        registerHandler(1, new Event("B2D_GET_EXTERNAL_CONFIGURATION"),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                File filesDir = getFilesDir();
                File dpAppend = new File(filesDir.getAbsolutePath(), "RB_DP_APPEND");
                sendEvent(new Event("D2B_SET_EXTERNAL_CONFIGURATION")
                .addVar(new EventVar("DMA_VAR_BUFFER_SIZE", 300000))
                .addVar(new EventVar("DMA_VAR_MAXSIZE", 300000000))
                .addVar(new EventVar("DMA_VAR_FILESIZE", (int)dpAppend.length())));
            }
        });


        registerHandler(1, new Event("B2D_MSG_PROXY_CONFIGURATION_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG, 
                new EventHandler(this) {
            @Override
            protected void genericHandler(Event ev) {
                sendEvent(new Event("D2B_MSG_PROXY_CONFIGURATION_RESPONSE")
                .addVar(new EventVar("DMA_VAR_DM_PROXY", ""))
                .addVar(new EventVar("DMA_VAR_DL_PROXY", "")));
            }
        });	

        registerHandler(1, new Event("B2D_REBOOT_TO_BOOT_PARTITION"), 
                UI_MODE_BOTH_FG_AND_BG,
                new EventHandlerIntent(this, RequestReboot.class));

        registerHandler(1, new Event("B2D_CONTINUE_UPDATE_AFTER_FOTA"), 
                UI_MODE_BOTH_FG_AND_BG,
                new AfterFotaUpdateHandler(this));

        // General behaviour for Device and Automotive
        // Silent, background or foreground, initiated by server, user, device. 
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI").addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 1)),
//                UI_MODE_BOTH_FG_AND_BG,
//                new EventHandler(this) {
//            @Override
//            protected void genericHandler(Event ev) {
//                sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
//            }
//        });

        // Non-silent, Non-critical, background, initiated by user, device, server.
//        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI")
//        .addVar(new EventVar("DMA_VAR_SCOMO_ISSILENT", 0))
//        .addVar(new EventVar("DMA_VAR_SCOMO_CRITICAL", 0)),
//        UI_MODE_BACKGROUND,
//        new InstallConfirmNotificationHandler(this));


        // register HTML5 or Android Activities
        if (isAutomotive) {
            Html5EventRegister();
        } else {
            AndroidEventRegister();
        }


        /////////////////////////////////
        // Micronet/DS 2016-08-18: Use our own handlers

        registerMicronetHandlers();

        // END Micronet/DS 2016-08-18: Use our own handlers
        /////////////////////////////////

    }

    void writeFile(byte[] aInput, File aOutputFile){
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(aOutputFile,true));
                output.write(aInput);
            }
            finally {
                if (output != null)
                    output.close();
            }
        }
        catch(FileNotFoundException ex){
            Log.d(LOG_TAG,"File not found.");
        }
        catch(IOException ex){
            Log.e(LOG_TAG,ex.toString());
        }
    }

    public byte[] readFile(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            int size = (int) file.length();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
            byte[] buffer = new byte[4096]; // Or whatever constant you feel like using
            int done = 0;
            while (done < size) {
                int read = in.read(buffer);
                if (read == -1) {
                    return null;
                }
                outputStream.write(buffer, 0, read);
                done += read;
            }
            in.close();
            return outputStream.toByteArray();
        }catch (IOException ex)	{
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            Log.e(LOG_TAG, ex.toString());	
            return null;
        }
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





    //////////////////////////////////////////////////////////////////////////
    // registerMicronetHandlers()
    //      added by Micronet/DS 2016-08-18
    //      most normal handlers are commented out and these are the ones that are used.
    //////////////////////////////////////////////////////////////////////////
    public void registerMicronetHandlers() {


        registerHandler(1, new Event("DMA_MSG_SCOMO_REBOOT_CONFIRM_REQUEST"),
                UI_MODE_BOTH_FG_AND_BG,
                MicronetConfirmHandler.getRebootRequestHandler(this));

        registerHandler(1, new Event("DMA_MSG_SCOMO_NOTIFY_DL_UI"),
                UI_MODE_BACKGROUND,
                MicronetConfirmHandler.getNotifyDownloadHandler(this));

        registerHandler(1, new Event("DMA_MSG_SCOMO_DL_CONFIRM_UI"),
                UI_MODE_BOTH_FG_AND_BG,
                MicronetConfirmHandler.getConfirmDownloadHandler(this));


        registerHandler(1, new Event("DMA_MSG_SCOMO_INS_CONFIRM_UI"),
                UI_MODE_BOTH_FG_AND_BG,
                MicronetConfirmHandler.getConfirmInstallHandler(this));

        registerHandler(1, new Event("DMA_MSG_SCOMO_SET_DL_TIMESLOT"),
                UI_MODE_BOTH_FG_AND_BG,
                MicronetConfirmHandler.getConfirmDLTimeSlotHandler(this));
    }


}
