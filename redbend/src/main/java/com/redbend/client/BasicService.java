/*
 *******************************************************************************
 *
 * BasicService.java
 *
 * Sends and receives events using intents to and from the BLL. Initializes the
 * SMM service and sets up the relevant resources.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
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
import java.util.HashMap;
import java.util.TreeSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.os.Handler;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import com.redbend.app.EventIntentService;
import com.redbend.app.Event;
import com.redbend.app.EventVar;
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
	
	private HashMap<String,Event> m_docEventsToSmm;
	private HashMap<String,Event> m_docEventsFromSmm;
	private AssetManager m_assets;
	private boolean m_enforceEventVarsValidate = false;

	private native void ipcSendEvent(byte msg[]);
	private native int initEngine(String filesDir, String configFile);
	
	class DevNodeValue{
		String value;
		boolean forceReplace;
		DevNodeValue(String inValue, boolean inForceReplace) {
			value = inValue;
			forceReplace = inForceReplace;
		}
	}
	private native void startSmm(DevNodeValue deviceId, String userAgent, DevNodeValue deviceModel, DevNodeValue deviceManufacturer);	
	private native void destroyEngine();
	private native void stopSmm();
	private int m_initEngineResult = -1;
	
	public static final String ASSETS_DIR = "files";
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
				String deviceIdStr = Ipl.getDeviceId(ctx);
				
				// Get UserAgent
				String userAgent =Ipl.getUserAgent(ctx);
				
				// Get device Model
				String deviceModelStr = Ipl.getDevModel();
				
				// Get device Manufacturer
				String manufacturerStr = Ipl.getManufacturer();

				//Start the SMM
				DevNodeValue deviceId = new DevNodeValue(deviceIdStr, true);
				DevNodeValue deviceModel = new DevNodeValue(deviceModelStr, false);
				DevNodeValue manufacturer = new DevNodeValue(manufacturerStr, false);
				// Call startSmm with null as deviceId/deviceModel/manufacturer
				// and the engine will call the appropriate IPL from Ipl.java
				startSmm(deviceId, userAgent, deviceModel, manufacturer);    
				
				// set Event Vars Validation
				Resources res = ctx.getResources();
				m_enforceEventVarsValidate = res.getBoolean(R.bool.enforceEventVarsValidation);
				Log.d(LOG_TAG,"Event vars validation is: " + m_enforceEventVarsValidate);
				
			}}, START_SMM_DELAY_MSEC);
	}
	
	boolean validateEventVars(HashMap<String, Event> hash, Event ev) {
		
		/* return true for release version*/
		if (m_enforceEventVarsValidate == true)
		{
			if(ev.varsCount()==0)
				return true;
			Event docEvent = hash.get(ev.getName());
			for (EventVar var : docEvent.getVars()){
				try
				{
					ev.getVar(var.getName());
				}catch(Exception e){
					Log.e(LOG_TAG,"##### Failed to find variable <" + var.getName() + "> in event <" + ev.getName() + ">.");
					Log.e(LOG_TAG,"##### Should this variable be declared optional in document (events.xml) ??");
					return false;
				}
			}
			Log.d(LOG_TAG, hash.getClass() + " " + ev.getClass());
		}
		return true;
	}
	
	//Send event from DIL to BLL
	@Override
	protected void sendEvent(Event ev) {
		if (m_initEngineResult != 0)
			return;
		try {
			if (!validateEventVars(m_docEventsToSmm, ev))
				throw new IOException("The event is not validated");
			ipcSendEvent(ev.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** method called from JNI on UI event receive */
	protected void recvEvent(byte ev[])
	{
		if (m_initEngineResult != 0) {
			Log.d(LOG_TAG, "recvEvent will be skipped");
			return;
		}
		try {
			Event event = new Event(ev);
			if (!validateEventVars(m_docEventsFromSmm, event))
				throw new IOException("The event is not validated");
			super.recvEvent(event);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error decoding received UI event");
		}
	}

	private static String[] getStringsArray(HashMap<String, Event> inArrayName){
		String[] array = new String[inArrayName.size()];
		inArrayName.keySet().toArray(array);
		return array;
	}
	
	private static TreeSet<String> parseXML(XmlResourceParser parser,
			String inArrayName) throws XmlPullParserException, IOException {
		
		String arrayName = null;
		TreeSet<String> outTreeSet = new TreeSet<String>();		
		int eventType = parser.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			if (eventType == XmlPullParser.START_TAG) {
				name = parser.getName();
				if (name.equals("array")) {
					arrayName = parser.getAttributeValue(null, "name");
					Log.d(LOG_TAG, "parseXML::START_TAG->arrayName: "
							+ arrayName);
				} else if (arrayName != null && arrayName.equals(inArrayName)) {
					if (name.equals("eventName")) {
						String currentEventName = parser.nextText();
						outTreeSet.add(currentEventName);
						Log.d(LOG_TAG,
								"parseXML::START_TAG->add currentEventName: "
										+ currentEventName + " to String array");
					}
				}
			}
			eventType = parser.next();
		}
		return outTreeSet;
	}

	@Override
	public void onCreate()	
	{
		super.onCreate();
		Log.d(LOG_TAG, "+onCreate");			
                
		if(!shouldCreateService())
		{
			Log.d(LOG_TAG, "onCreate: running under Jsystem and file /sdcard/stop_smm exists - not creating the service!!!");
			return;
		}
		
		Log.d(LOG_TAG, "onCreate: creating the service!!!");
		m_assets = getAssets();
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
		
		//String dma_config_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/dma_config.txt"; //e.g. "/storage/emulated/0/dma_config.txt"
		String dma_config_path = null;
		// initialize the engine	
		m_initEngineResult = initEngine(fileDir.getAbsolutePath(), dma_config_path);
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

		Resources res = getResources();
		XmlResourceParser parser =res.getXml(R.xml.events);
		XmlResourceParser parser2 =res.getXml(R.xml.events);
		m_docEventsToSmm = initDocumentedVars("eventsToSmm", parser);
		m_docEventsFromSmm = initDocumentedVars("eventsFromSmm", parser2);
		parser.close();
		parser2.close();

		String[] array = getStringsArray(m_docEventsFromSmm);
		for (String event : array)
			addEventFromSmm(event);
		
		array = getStringsArray(m_docEventsToSmm);
		for (String event : array)
			addEventToSmm(event);
		
		register(INTENT_START_ACTION);
		Log.d(LOG_TAG, "-onCreate");
	}	

	HashMap<String, Event> initDocumentedVars(String inArrayName, XmlResourceParser parser) {
		Log.d(LOG_TAG, "+getStringsArray=>" + inArrayName);
		HashMap<String, Event> map = null;

		try {
			
			map = parseXML2(parser, inArrayName);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		if(map == null){
			Log.e(LOG_TAG, "-initDocumentedVars=>return null");
			return null;
		}
		Log.d(LOG_TAG, "-initDocumentedVars");
		return map;
	}
	
	
	private static HashMap<String, Event> parseXML2(XmlResourceParser parser,
			String inArrayName) throws XmlPullParserException, IOException {
		
		String arrayName = null;
		HashMap<String, Event> outTreeSet = new HashMap<String, Event>();		
		int eventType = parser.getEventType();
		Event event = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			if (eventType == XmlPullParser.START_TAG) {
				name = parser.getName();
				if (name.equals("array")) {
					arrayName = parser.getAttributeValue(null, "name");
					Log.d(LOG_TAG, "parseXML arrayName: "
							+ arrayName);
				} else if (arrayName != null && arrayName.equals(inArrayName)) {
					if (name.equals("eventName")) {
						if (event != null)
							outTreeSet.put(event.getName(), event);
						String currentEventName = parser.nextText();
						event = new Event(currentEventName);
						Log.d(LOG_TAG, "parseXML currentEventName: " + currentEventName);
					}
					else if (name.equals("varName")) {
						String isValidate = parser.getAttributeValue(null, "validate");
						String varName = parser.nextText();
						if (event != null) {
							if (isValidate == null || isValidate.equals("true")){ 
								event.addVar(new EventVar(varName, 0));
								Log.d(LOG_TAG, "parseXML varName: "	+ varName);
							}
							else
								Log.d(LOG_TAG, "parseXML varName: "	+ varName + " validation ignored");
						}
					}
				}
					
			}
			eventType = parser.next();
		}
		if (event != null)
			outTreeSet.put(event.getName(), event);
		return outTreeSet;
	}

    @Override
	protected void shutdown() {
    	destroyEngine();
    	stopSmm();
    	Log.d("BasicService", "-shutdown()");
 	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(alarmReceiver);
		Log.d("BasicService", "-onDestroy()");
	}
	
	public static void copyFiles(InputStream in, File outFile) throws IOException
	{
		FileOutputStream out = new FileOutputStream(outFile); //open file in files dir
		byte[] buffer = new byte[BYTE_ARRAY_SIZE];
		int count;

		while ((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);
		out.close();
	}

	public void createAssets(Context ctx) throws IOException
	{
		String files[] = m_assets.list(ASSETS_DIR);
		String filePath = ctx.getFilesDir().getAbsolutePath();
		
		for (int i = 0; i < files.length; i++)
		{
			String fileName = files[i];
			Log.d(LOG_TAG, "createAssets::Found asset: " + fileName);
			try {
				FileInputStream fd = new FileInputStream(new File(filePath, fileName));//open file in files dir to check if exist
				Log.d(LOG_TAG, "Filename '" + fileName + "' already exists");
				fd.close();
			} catch (FileNotFoundException e) {
				Log.d(LOG_TAG, "Filename '" + fileName + "' doesn't exist, creating from asset");
				InputStream assets_fd = m_assets.open(ASSETS_DIR + "/" + fileName);
				copyFiles(assets_fd, new File(filePath, fileName));
				assets_fd.close();
			}
		
		}
	}
	
	private static void deleteFiles(File inDir) {		
		if (inDir == null) {
			return;
		}
		Log.d(LOG_TAG, "+deleteFiles::dir name is " + inDir.getName());
		if (inDir.isDirectory()) {
			Log.d(LOG_TAG, "Is a dir");
			String[] children = inDir.list();
			for (int i = 0; i < children.length; i++) {
				Log.d(LOG_TAG, "deleteFiles:: file: " + children[i]);
				new File(inDir, children[i]).delete();
			}
		}
		Log.d(LOG_TAG, "-deleteFiles");
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
		Log.d(LOG_TAG, "+deleteUserData");
		deleteFiles( getFilesDir());
		Log.d(LOG_TAG, "-deleteUserData");
	}
	
	private boolean shouldCreateService(){
		Resources res = getResources();
		boolean isJsystem = res.getBoolean(R.bool.isJsystem);
		Log.d(LOG_TAG, "is" + (isJsystem ? " " : " NOT ") + "running under Jsystem");
		if(isJsystem) 
		{
			File file = 
				new File("/mnt/sdcard/stop_smm");
			if(file.exists())
				return false;
		}
		return true;
	}
}
