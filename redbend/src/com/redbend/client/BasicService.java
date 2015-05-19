/*
 *******************************************************************************
 *
 * BasicService.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import android.os.Handler;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.redbend.app.EventIntentService;
import com.redbend.app.Event;
import com.redbend.vdm.comm.VdmComm;
import com.redbend.vdm.comm.VdmCommException;
import com.redbend.vdm.comm.VdmCommFactory;

/**
 * Send and receive events using intents to and from the BLL. Initialize the
 * SMM service and setup the relevant resources. Invoked after the device boots
 * up.
 */
public class BasicService extends EventIntentService
{
	private static final String INTENT_START_ACTION = "com.redbend.client.START_CLIENT";

	public static final String CLEAR_DATA_EVENT = "DMA_MSG_USER_CLEAR_DATA";
	public static final String STOP_CLIENT_EVENT = "DMA_MSG_STOP_CLIENT_SERVICE";
	
	private final static String LOG_TAG = "BasicService";
	
	private static final String INTENT_SET_ALARM = "com.redbend.client.SET_ALARM";
	private VdmComm m_comm;
	private static final int START_SMM_DELAY_MSEC = 5*1000; //5 seconds
	private static final String ALARM_ID = "alarmId";
	private AlarmManager m_alarmManager;

	private native void ipcSendEvent(byte msg[]);
	private native int initEngine(String filesDir);
	private native void startSmm(String deviceId, String userAgent, String deviceModel, String deviceManufacturer);	
	private native void destroyEngine();
	private native void stopSmm();
	private int m_initEngineResult = -1;
	
	private static final String ASSETS_DIR = "files";
	public static final int BYTE_ARRAY_SIZE = 1024;
	
	/** native function that is called to inform SMM of expired alarm */
	private native void alarmExpire(int alarmId);
	
	private final BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(m_initEngineResult != 0)
				return;
			int alarmId = intent.getIntExtra(ALARM_ID, 0);

			if (alarmId != 0) {
				Log.i(LOG_TAG, "Alarm ID " + alarmId + " expired");
				alarmExpire(alarmId);
			}
		}
	};
	
	private final static Intent getAlarmIntent() {
		return new Intent(INTENT_SET_ALARM);
	}

	/** Called from JNI in order to set RTC alarm */
	protected void setAlarm(int id, int secs) {
		Intent i = getAlarmIntent();

		i.putExtra(ALARM_ID, id);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, secs);
		Log.i(LOG_TAG, "Setting Alarm ID " + id +" for " + cal.getTime());
		m_alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				PendingIntent.getBroadcast(this, id, i, PendingIntent.FLAG_UPDATE_CURRENT));
	}

	/** Called from JNI in order to reset RTC alarm */
	protected void resetAlarm(int id) {
		Intent i = getAlarmIntent();

		Log.i(LOG_TAG, "Resetting Alarm ID " + id);
		m_alarmManager.cancel(
				PendingIntent.getBroadcast(getApplicationContext(), id, i, PendingIntent.FLAG_UPDATE_CURRENT));
	}

	@Override
	protected void start() {
		// In order to start SMM we need the IMEI (CR 177)
		// Sometimes in boot the IMEI is not ready
		// so wait a few seconds before inquire the IMEI and start the SMM
		Handler handlerTimer = new Handler();
		handlerTimer.postDelayed(new Runnable(){
			public void run() {
				Log.d(LOG_TAG,"Running after delay:" + START_SMM_DELAY_MSEC);
				
				if (m_initEngineResult != 0)
					return;

				Context ctx = getApplicationContext();
				// Get DeviceId
				String deviceId = Ipl.getDeviceId(ctx);
				
				// Get UserAgent
				String userAgent =Ipl.getUserAgent(ctx);
				
				// Get device Model
				String deviceModel = Ipl.getDevModel();
				
				// Get device Manufacturer
				String manufacturer = Ipl.getManufacturer();
				
				Log.d(LOG_TAG,"deviceId: " + deviceId + "userAgent: " + userAgent + 
						"deviceModel: " + deviceModel + "manufacturer: " + manufacturer);
				
				//Start the SMM
				startSmm(deviceId, userAgent, deviceModel, manufacturer);             
			}}, START_SMM_DELAY_MSEC);
	}

	@Override
	protected void sendEvent(Event ev) {
		if (m_initEngineResult != 0)
			return;
		try {
			ipcSendEvent(ev.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** method called from JNI on UI event receive */
	protected void recvEvent(byte ev[])
	{
		if (m_initEngineResult != 0) {
			Log.e(LOG_TAG, "recvEvent will be skipped");
			return;
		}

		try {
			super.recvEvent(new Event(ev));
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error decoding received UI event");
		}
	}

	@Override
	public void onCreate()	
	{
		super.onCreate();
		Log.e(LOG_TAG, "+onCreate");
			
		try {
			createAssets(this);
		} catch (IOException e) {
			Log.e(LOG_TAG, "-onCreate::Error reading the assets");
		}

		// load the needed shared libraries
		System.loadLibrary("smm");
		
		m_alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		registerReceiver(alarmReceiver, new IntentFilter(INTENT_SET_ALARM));

		File fileDir = getFilesDir();
		if (fileDir == null)
        {
			Log.e(LOG_TAG, "-onCreate::Failed to get files dir");
			return;
		}
        
		/* WORKAROUND: To avoid a scenario where BasicService sends a broadcast
		 * intent to ClientService before ClientSerivce has started to run. */
		Log.d(LOG_TAG, "Calling startService for ClientService");
		startService(new Intent(this, ClientService.class));

		// initialize the engine	
		m_initEngineResult = initEngine(fileDir.getAbsolutePath());
		if (m_initEngineResult != 0) {
			Log.e(LOG_TAG, "-onCreate::initEngine - 0x" + Integer.toHexString(m_initEngineResult) + 
				" failed, return");
			return;
		}
		// initialize the communication PL
		try
		{
			m_comm = new VdmComm(new VdmCommFactory());
			m_comm.setConnectionTimeout(20);
		}
		catch (VdmCommException e)
		{
			e.printStackTrace();
		}

		Resources r = getResources();
		for (String event : r.getStringArray(R.array.eventsFromSmm))
			addEventFromSmm(event);
		for (String event : r.getStringArray(R.array.eventsToSmm))
			addEventToSmm(event);

		register(INTENT_START_ACTION);
		Log.e(LOG_TAG, "-onCreate");
	}	

    @Override
	protected void shutdown() {
    	destroyEngine();
 	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(alarmReceiver);
		Log.d("BasicService", "-onDestroy()");
	}
	
	private static void copyFiles(InputStream in, FileOutputStream out) throws IOException
	{
		byte[] buffer = new byte[BYTE_ARRAY_SIZE];
		int count;

		while ((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);
	}

	public static void createAssets(Context ctx) throws IOException
	{
		AssetManager assets = ctx.getAssets();
		String files[] = assets.list(ASSETS_DIR);

		for (int i = 0; i < files.length; i++)
		{
			String file = files[i];

			Log.d(LOG_TAG, "createAssets::Found asset: " + file);
			try {
				FileInputStream fd = ctx.openFileInput(file);

				Log.d(LOG_TAG, "Filename '" + file + "' already exists");
				fd.close();
			}
			catch (FileNotFoundException e)
			{
				FileOutputStream files_fd = ctx.openFileOutput(file, MODE_PRIVATE);
				InputStream assets_fd = assets.open(ASSETS_DIR + "/" + file);

				Log.d(LOG_TAG, "Filename '" + file + "' doesn't exist, creating from asset");
				copyFiles(assets_fd, files_fd);
				files_fd.close();
				assets_fd.close();
			}
		}
	}
	
	private static void deleteFiles(File inDir) {		
		if (inDir == null) {
			return;
		}
		Log.e(LOG_TAG, "+deleteFiles::dir name is " + inDir.getName());
		if (inDir.isDirectory()) {
			Log.e(LOG_TAG, "Is a dir");
			String[] children = inDir.list();
			for (int i = 0; i < children.length; i++) {
				Log.d(LOG_TAG, "deleteFiles:: file: " + children[i]);
				new File(inDir, children[i]).delete();
			}
		}
		Log.e(LOG_TAG, "-deleteFiles");
	}

	private void sendStopClientServiceEvent(){
		Event event = new Event(STOP_CLIENT_EVENT);
		Intent i = event.createIntent();
		sendBroadcast(i, PERMISSION);
	}
	
	@Override
	protected boolean processEvent(Event ev) {
		String eventName = ev.getName();

		if (!CLEAR_DATA_EVENT.equals(eventName))
			return false;

		shutdown();
		//now stop client service
		sendStopClientServiceEvent();
		deleteUserData();
		stopSelf();
		return true;
	}

	private void deleteUserData()
	{
		Log.e(LOG_TAG, "+deleteUserData");
		deleteFiles( getFilesDir());
		Log.e(LOG_TAG, "-deleteUserData");
	}
}
