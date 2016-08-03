/*
 *******************************************************************************
 *
 * SmmService.java
 *
 * Manages the DIL. An abstract implementation of the Android service.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.*;
import java.util.*;

import android.os.*;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.lang.Process;
import java.lang.ref.WeakReference;

import android.annotation.TargetApi;
import android.util.Log;

@TargetApi(11)
abstract public class SmmService extends Service implements EventHandlerContext {

    public final String LOG_TAG = getClass().getSimpleName();

    /** extra asking to send event when starting the service */
    public static final String startServiceMsgExtra = "serviceStartMsg";
    public static final String startServiceFromSmmReceiveExtra = "startServiceFromSmmReceive";
    public static final String receivedEventExtra = "receivedEventExtra";
    public static final String returnFromBackground = "returnFromBackground";
    public static final String flowIdExtra = "flowId";
    public static final String deviceBooted = "deviceBooted";
    public static final String PREFS = "PREFERENCES";
    public static final String ALARM_TIME = "ALARM_TIME";

    /** application must be either in foreground or background,
     *  event can be handled in while the application is in foreground,
     *  background or both */
    protected static final int UI_MODE_BACKGROUND = 1;
    protected static final int UI_MODE_FOREGROUND = 2;
    protected static final int UI_MODE_BOTH_FG_AND_BG = UI_MODE_BACKGROUND | UI_MODE_FOREGROUND;

    /* messages used to pass data/events between the service and UI activities */
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SEND_EVENT_TO_SM = 3;
    static final int MSG_START_ACTIVITY = 4;
    static final int MSG_SEND_EVENT_TO_UI = 5;
    static final int MSG_ACTIVITY_PAUSED = 6;
    static final int MSG_ACTIVITY_RESUMED = 7;
    static final int MSG_SET_TIMER = 8;
    static final int MSG_TIMER_EXPIRED = 9;
    static final int MSG_END_TASK = 10;
    static final int MSG_ACTIVITY_FINISHED = 11;
    static final int MSG_ACTIVITY_NEW_INTENT = 12;

    /*
     * Default flow ID. Used in registerHandler().
     * The default flow has no GUI.
     *
     * When GUI is required, define a new flow for the handler.
     * Flow IDs should be defined in subclasses of this class.
     */
    protected static final int DEFAULT_FLOW = 1;

    private Hashtable<Integer,FlowManager> mFlows = new Hashtable<Integer,FlowManager>(2);
    private EventMultiplexer mEventMux;

    private EventReceiver mEventReceiver;	// Receiver that gets events from EventIntentService and starts smm service
    private IntentFilter mEventReceiverFilter;
    private boolean mHasIntentReceiver = false;
    private ComponentName mSendToComponentName;


    /** If the next flow will be started in foreground */
    public void startFlowInForeground(int flowId) {
        FlowManager flow = mFlows.get(flowId);

        if (flow.isEmpty() && !flow.isForeground()) {
            flow.setForeground();
            Log.i(LOG_TAG, "Setting ui mode to foreground.");
        }
    }

    /** If the next flow will be started in background */
    public void startFlowInBackground(int flowId) {
        FlowManager flow = mFlows.get(flowId);

        if (flow.isEmpty() && !flow.isBackground()) {
            flow.setBackground();
            Log.i(LOG_TAG, "Setting ui mode to background.");
        } else {
            Log.d(LOG_TAG, "failed to set ui to background: flow.isEmpty=" +
                    flow.isEmpty() + " flow.isBackground=" + flow.isBackground());
        }
    }

    private static class EventReceiver extends WakefulBroadcastReceiver {

        private static final String LOG_TAG = "ClientService.Event";
        ComponentName mService;

        EventReceiver(ComponentName service) {
            this.mService = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Event evt;
            Intent i = new Intent();

            try {
                evt = new Event(intent);
            } catch (IOException e) {
                Log.i(LOG_TAG, "Error receiving an event, " + e.getMessage());
                return;
            }

            i.setComponent(mService);
            try {
                i.putExtra(SmmService.receivedEventExtra, evt.toByteArray());
            }
            catch (IOException e) {
                Log.i(LOG_TAG, "Error serializing event, " + e.getMessage());
                return;
            }

            Log.i(LOG_TAG, "starting Service with:" + intent.getAction() + " evnt:" + evt.getName());
            startWakefulService(context, i);
        }
    }

    /**
     * Handler of incoming messages from clients.
     */
    private static class ActivityMsgHandler extends Handler {

        private final WeakReference<SmmService> mSmmService;
        private final static String LOG_TAG = "SmmService.ActivityMsgHandler";

        public ActivityMsgHandler(SmmService s) {
            mSmmService = new WeakReference<SmmService>(s);
        }

        @Override
        public void handleMessage(Message msg) {
            Messenger client = msg.replyTo;
            SmmService smmService = mSmmService.get();

            if (smmService == null)
                return;

            int flowId = (msg.what >> 8) & 0xff; // Mask for flow ID
            FlowManager flow = smmService.mFlows.get(flowId);

            switch (msg.what & 0xff) {
            case MSG_REGISTER_CLIENT:
                flow.add(client);
                FlowUtils.dLog(LOG_TAG, flowId, "Registering Activity #" + flow.size() + " client:" +
                        Integer.toHexString(client.hashCode()));

                if (flow.isBackground()) {
                    FlowUtils.dLog(LOG_TAG, flowId, "activity forced to return to foreground, removing the notification");
                    flow.setForeground();
                }
                break;
            case MSG_SEND_EVENT_TO_SM:
                smmService.sendEvent((Event) msg.obj);
                break;
            case MSG_UNREGISTER_CLIENT:
                int index = flow.remove(client);

                FlowUtils.dLog(LOG_TAG, flowId, "Unregistering Activity #" + (index+1) + " client:" +
                        Integer.toHexString(client.hashCode()));

                Boolean isStartingNewActivity = (Boolean) msg.obj;
                if (!isStartingNewActivity.booleanValue() || smmService.mFlows.size() != 1) {
                    // this is the regular flow
                    break;
                }
                if (flow.isBackground()) {
                    FlowUtils.dLog(LOG_TAG, flowId, "Requested to start a new activity while in background");
                    break;
                }
                FlowUtils.dLog(LOG_TAG, flowId, "Starting a new activity at the same time going to background");
                //$FALL-THROUGH$ continue with next case
            case MSG_ACTIVITY_PAUSED:
                FlowUtils.dLog(LOG_TAG, flowId, "activity paused, application goes to background");
                flow.setBackground();
                break;
            case MSG_ACTIVITY_RESUMED:
                FlowUtils.dLog(LOG_TAG, flowId, "activity resumed, application returned to foreground");
                flow.setForeground();
                break;
            case MSG_SET_TIMER:
                smmService.setTimer((Long)msg.obj);
                break;
            case MSG_TIMER_EXPIRED:
                /* XXX the service shouldn't implement logic that sends events */
                /* XXX the timer event shouldn't contain any variable */
                FlowUtils.dLog(LOG_TAG, flowId, "timer expired, sending event");
                break;
            case MSG_ACTIVITY_FINISHED:
                boolean finishingOnStop = (Boolean)msg.obj;
                /* The activity informed us that it has finished, so we cannot
                 * send any more events to it */
                flow.remove(client);
                flow.reset(finishingOnStop);

                /* reset the foreground/background configuration of the task */
                if (!finishingOnStop)
                    flow.setForeground();

                FlowUtils.dLog(LOG_TAG, flowId, "Activity has finished, clear the flow data, client:" +
                        Integer.toHexString(client.hashCode()));
                flow.removeRoot(client);
                break;
            default:
                super.handleMessage(msg);
                break;
            }
        }
    }

    class CancelTimerHandler extends EventHandler {
        public CancelTimerHandler(Context ctx) {
            super(ctx);
        }

        @Override
        protected void genericHandler(Event ev) {
            ((SmmService)ctx).cancelTimer();
        }
    }

    /** this method is called through the SM and Event Multiplexer, the SM
     * logic implements UI events posting through the SM exec thread, which
     * means they are synchronized */
    public void exec(EventHandler handler, Event ev, int flowId, int uiMode) {
        FlowManager flow = mFlows.get(flowId);

        flow.handle(handler, ev, uiMode);
    }

    private final Handler mHandler = new ActivityMsgHandler(this);

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(mHandler);

    /** method called on UI event receive */
    protected void recvEvent(Event ev) {
        Log.d(LOG_TAG, "Received event " + ev.getName());
        mEventMux.handleEvent(ev);
    }

    /** used to send events to SMM */
    abstract public void sendEvent(Event ev);

    /** register a handler to event from the SMM */
    protected void registerHandler(int flowId, Event ev, int uiMode, EventHandler h) {
        if (!mFlows.containsKey(flowId))
            mFlows.put(flowId, new FlowManager(flowId, this, mMessenger));

        mEventMux.addEventHandler(flowId, ev, uiMode, h);
        if (mHasIntentReceiver)
            mEventReceiverFilter.addAction(Event.intentActionPrefix + ev.getName());
    }

    protected final void setBackgroundNotification(int flowId, int icon,
            CharSequence title, CharSequence text) {
        FlowManager flow = mFlows.get(flowId);

        flow.setBackgroundNotif(this, icon, title, text);
    }

    @Override
    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EventMultiplexer WakeLock");

        mEventMux = new EventMultiplexer(this, wl);

        /* XXX no handlers should be defined on this level, in the future,
         * the timers will be handled exclusively by the SM Manager
         * 1 stands for the default flow */
        registerHandler(DEFAULT_FLOW, new Event("DMA_MSG_INT_CANCEL_TIMER"),
                UI_MODE_BOTH_FG_AND_BG, new CancelTimerHandler(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHasIntentReceiver)
            unregisterReceiver(mEventReceiver);
        mEventMux.destroy();
    }

    protected void defineIntentReceiver(String receiverPkgName, String receiverClassName, String componentPkgName) {
        mHasIntentReceiver = true;
        Log.i(LOG_TAG, "Service has Intent receiver. Component name is: Pkg = " + componentPkgName + ", class = " + getClass().getName());
        mEventReceiver = new EventReceiver(new ComponentName(getApplicationContext(), getClass()));
        mEventReceiverFilter = new IntentFilter();


        Log.d(LOG_TAG, "Component to send to from Pkg " + componentPkgName + " to pkg "+ receiverPkgName + " class " + receiverClassName);
        String fullClassName = new String (receiverPkgName + "." + receiverClassName);
        Log.d(LOG_TAG, "FullClassName is: " + fullClassName);
        mSendToComponentName = new ComponentName(componentPkgName, fullClassName);
        Log.d(LOG_TAG, "ComponentName is " + mSendToComponentName.toString());
    }

    protected void registerEventReceiver() {
        for (Iterator<String> i = mEventReceiverFilter.actionsIterator(); i.hasNext(); )
            Log.d(LOG_TAG, "eventReceiver filter=" + i.next());
        registerReceiver(mEventReceiver, mEventReceiverFilter, EventIntentService.PERMISSION, null);
    }

    protected void sendIntentEvent(Event ev) {
        Log.d(LOG_TAG, "SmmService Transmitting event: " + ev.getName());
        Intent intent = ev.createIntent();
        Log.d(LOG_TAG, "starting service: " + mSendToComponentName.toString());
        intent.setComponent(mSendToComponentName);
        startService(intent);
    }

    private void restoreAlarm() {
        // check pending alarm after reboot
        if (!getSharedPreferences(PREFS, 0).contains(ALARM_TIME))
            return;

        long alarmTime = getSharedPreferences(PREFS, 0).getLong(ALARM_TIME, 0);
        setTimer(alarmTime);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        UserHandle uh = android.os.Process.myUserHandle();
        Context context = getApplicationContext();
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        long userSerialNumber = 0;
        if(null != um)
        {
            userSerialNumber = um.getSerialNumberForUser(uh);
            if (userSerialNumber != 0)
            {
                Log.d(LOG_TAG, "Only the primary user can run Software Management");
                return START_NOT_STICKY;
            }
        }
        if(intent == null){
            return START_STICKY_COMPATIBILITY;
        }

        byte receivedEvent[] = intent.getByteArrayExtra(receivedEventExtra);
        if (receivedEvent != null) {
            try {
                recvEvent(new Event(receivedEvent));
                WakefulBroadcastReceiver.completeWakefulIntent(intent);
            } catch (IOException e) {
                Log.i(LOG_TAG, "Error receiving an event, " + e.getMessage());
            }
            return START_STICKY_COMPATIBILITY;
        }

        int flowId = intent.getIntExtra(flowIdExtra, 0);
        FlowUtils.dLog(LOG_TAG, flowId, "Received start id " + startId + ": " + intent);
        boolean startedFromSmmReceive = intent.getBooleanExtra(startServiceFromSmmReceiveExtra, false);

        Event event = null;
        try {
            byte eventExtra[] = intent.getByteArrayExtra(startServiceMsgExtra);
            if (eventExtra != null)
                event = new Event(eventExtra);
        } catch (IOException error) {
            error.printStackTrace();
        }

        /* When the service is stared with flowId, this means an application
         * requested to start a flow */
        if (flowId != 0) {
            FlowManager flow = mFlows.get(flowId);

            if (intent.getBooleanExtra(returnFromBackground, false)) {
                Log.i(LOG_TAG, "onStart: Application requested to return to foreground");
                flow.returnToForeground();
                if (event != null)
                    sendEvent(event);
            }
            /* if the flow is already executed and showing something, then just restore
             * to foreground, otherwise, send the provided initial event */
            else if (flow.isEmpty()) {
                Log.i(LOG_TAG, "onStart: User started the application, sending an initial event");
                if (event != null)
                    sendEvent(event);
                else
                    FlowUtils.dLog(LOG_TAG, flowId, "ERROR: Tried to start a flow without an initial event");
            }
            else {
                Log.i(LOG_TAG, "onStartCommand: User started an active application, request to return to foreground");
                flow.returnToForeground();
            }
        } else if (event != null) {
            // if no flowId is provided then unconditionally send the event
            Log.d("SMM", "Sending event " + event.getName());
            sendEvent(event);
        }

        if (intent.getBooleanExtra(deviceBooted, false))
            restoreAlarm();

        if (startedFromSmmReceive)
            WakefulBroadcastReceiver.completeWakefulIntent(intent);

        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void setTimer(long alarmTime) {
        long delay = alarmTime - System.currentTimeMillis();

        Log.d(LOG_TAG, "setting delayed message after "+ delay/1000 + " s");

        if (delay > 0)
            mHandler.sendEmptyMessageDelayed(MSG_TIMER_EXPIRED, delay);
        else
            mHandler.sendEmptyMessage(MSG_TIMER_EXPIRED);

        // saves the alarm time to preferences
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, 0).edit();
        editor.putLong(ALARM_TIME, alarmTime);
        editor.commit();
    }

    /** Cancel the timer. Used by SM Handler, when the SM goes to another state,
     * while in deferred state */
    private void cancelTimer() {
        Log.d(LOG_TAG, "removed delayed timer messages");
        mHandler.removeMessages(MSG_TIMER_EXPIRED);

        // removes the alarm from preferences
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, 0).edit();
        editor.remove(ALARM_TIME);
        editor.commit();
    }


    /** Send a finish request to the top activity.
     *  Intended to be called from an event handler */
    public void requestFinishFlow(int flowId, boolean noTransition) {
        FlowManager flow = mFlows.get(flowId);

        if (flow == null) {
            Log.e(LOG_TAG, "Requested to finish flow on invalid flow " + flowId);
            return;
        }
        flow.requestFinishFlow(noTransition);
    }

    public void finishAllFlows(boolean noTransition)
    {
        if (mFlows.isEmpty()) {
            return;
        }
        for (FlowManager flow : mFlows.values()) {
            if (flow != null && flow.isEmpty() == false)
                flow.requestFinishFlow(noTransition);
        }
    }

    protected boolean isFlowInForeground(int flowId)
    {
        if (mFlows.get(flowId) != null)
            return mFlows.get(flowId).isForeground();
        return false;
    }

    public static String getSystemProperty(String property, String defaultValue)
    {
        String line;
        Process proc = null;

        try {
            proc = Runtime.getRuntime().exec("getprop " + property);
        } catch (java.io.IOException e) {
            return defaultValue;
        }
        try {
            BufferedReader bis = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            line = bis.readLine();
        } catch (java.io.IOException e) {
            proc.destroy();
            return defaultValue;
        }
        proc.destroy();

        /* if empty string then return the default value */
        if (line == null || line.compareTo("") == 0)
            return defaultValue;
        return line;
    }

    /**
     * Get a flag indicating whether a permission was granted.
     *
     * @param permName permission name
     * @return true if the permission was granted, false otherwise
     *
     */
    public static boolean isPermissionGranted(String permName, Context inContext) {
        PackageManager pm = inContext.getPackageManager();
        boolean isGranted = (pm.checkPermission(permName,
                inContext.getPackageName()) == PackageManager.PERMISSION_GRANTED);

        return isGranted;
    }
}
