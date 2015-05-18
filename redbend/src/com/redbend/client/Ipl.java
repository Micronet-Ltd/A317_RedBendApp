/*
 *******************************************************************************
 *
 * Ipl.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * Get the domain name and domain PIN from external storage, usually an SD card.
 */
public class Ipl
{
	protected final static String LOG_TAG = "IPL";
	protected final static String AUTO_SELF_REG_FILE_PATH = "/private/Credentials.txt";
	protected static boolean mExternalStorageAvailable = false;

	//The Automatic self registration info file structure is as follows:
	//It will have two lines - the 1st containing the domain name and the 2nd
	//the pin code.
	public static int iplGetAutoSelfRegDomainInfo(String []autoSelfRegDomainInfo) {
		int err = -1;
		//Check current external storage state
		updateExternalStorageState();
		if (mExternalStorageAvailable) {
			BufferedReader br = null;
			File dir = Environment.getExternalStorageDirectory();
			Log.e(LOG_TAG, "iplGetAutoSelfRegDomainInfo - dir = " + dir);
			try {
				File autoSelfRegInfoFile = new File(dir, AUTO_SELF_REG_FILE_PATH);
				br = new BufferedReader(new FileReader(autoSelfRegInfoFile));
				// Try to read both values
				if ((autoSelfRegDomainInfo[0] = br.readLine()) != null &&
					(autoSelfRegDomainInfo[1] = br.readLine()) != null)
				{
						// Return 0 only in success
						err = 0;
				}
			} catch (IOException e) {
				Log.i(LOG_TAG, "iplGetAutoSelfRegDomainInfo - IOException err=" + e.getMessage());
			} catch (NullPointerException e) {
				Log.e(LOG_TAG, "iplGetAutoSelfRegDomainInfo - NullPointerException err=" + e.getMessage());
			}

			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				Log.w(LOG_TAG, "iplGetAutoSelfRegDomainInfo - IOException err=" + e.getMessage());
			}
		}
		return err;
	}

	//Checking external storage (typically a SD card) for state and permissions
	protected static void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		mExternalStorageAvailable = false;
		if (Environment.MEDIA_MOUNTED.equals(state) ||
				Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
		}
	}
	
	public static String getDevModel() {
		return Build.MODEL;
	}
	
	public static String getManufacturer() {
		return Build.MANUFACTURER;
	}
	
	public static String getFwVersion() {
		return Build.DISPLAY;
	}
	
	public static String getDeviceId(Context ctx) {
		
		/* Get the telephony manager */
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		
		//Get device IMEI
		String deviceId = tm.getDeviceId();

		// Tablet might not be able to get imei
		if ( deviceId == null || deviceId.equals("") ) {
			WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();
			if (info != null) deviceId = info.getMacAddress();
		}

		Log.d(LOG_TAG,"DeviceId:" + deviceId);
		return deviceId;
	}
	
	public static String getUserAgent(Context ctx) {
		return getDeviceId(ctx);
	}	
	
	public static String getDefaultValue(String Uri)
	{
		Log.d(LOG_TAG, "getDefaultValue URI: " + Uri);
		String res = null;
		if (Uri.equals("./Ext/RedBend/BootupPollingInterval"))
			res =  "60";
		else if(Uri.equals("./Ext/RedBend/RecoveryPollingInterval"))
			res = "1440";
		else if(Uri.equals("./Ext/RedBend/UserInteractionTimeoutInterval"))
			res = "1440";
		else if(Uri.equals("./Ext/RedBend/PostponePeriod"))
			res = "60";
		else if(Uri.equals("./Ext/RedBend/PostponeMaxTimes"))
			res = "3";
		Log.d(LOG_TAG, "getDefaultValue, URI:"+ Uri + " value:" + res);
		return res;
	}
}
