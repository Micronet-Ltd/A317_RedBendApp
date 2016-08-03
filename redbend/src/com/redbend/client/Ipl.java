/*
 *******************************************************************************
 *
 * Ipl.java
 *
 * Gets the domain name and domain PIN from external storage, usually an SD
 * card.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	private static final String DEVICE_INFO_PREFS = "device_info_prefs";
	private static final String DEVICE_ID_KEY = "device_id";
	private static final  int MAC_ADDRESS_LENGTH = 17;
	private static final String TEST_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + 
			"/swmDebug.txt";
	private static final String TEST_MODEL = "TEST_MODEL";
	private static final String TEST_MANUFACTURER = "TEST_MANUFACTURER";
	private static final String TEST_FW_VERSION = "TEST_FW_VERSION";
	private static final String TEST_VIN = "TEST_VIN";
	
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
			Log.d(LOG_TAG, "iplGetAutoSelfRegDomainInfo - dir = " + dir);
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
		String model = readFromBackdoorFile(TEST_MODEL);
		if (model == null)
			return Build.MODEL;
		return model;
	}

	public static String getManufacturer() {
		String manufacturer = readFromBackdoorFile(TEST_MANUFACTURER);
		if (manufacturer == null)
			return Build.MANUFACTURER;
		return manufacturer;
	}
	
	public static String getFwVersion() {
		String fwVersion = readFromBackdoorFile(TEST_FW_VERSION);
		if (fwVersion == null)
			return Build.DISPLAY;
		return fwVersion;
	}
	
	public static String getDeviceId(Context ctx) {
		Log.d(LOG_TAG,"IPL:getDeviceId");
		
		SharedPreferences settings = ctx.getSharedPreferences(DEVICE_INFO_PREFS, 0);
	    String deviceId = settings.getString(DEVICE_ID_KEY, null);
	    if (deviceId == null){
	    	/* Get the telephony manager */
			TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
			
			//Get device IMEI
			deviceId = tm.getDeviceId();

			// Tablet might not be able to get IMEI
			if ( deviceId == null || deviceId.equals("") ) {
				WifiManager manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = manager.getConnectionInfo();
				if (info != null) {
					String mac = info.getMacAddress();
					if (mac != null)
						deviceId = parseMac(mac);
					else
						deviceId=  Build.SERIAL;
				}
			}
			
			//save the new device ID
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(DEVICE_ID_KEY, deviceId);
			editor.commit();
			Log.d(LOG_TAG,"new DeviceId:" + deviceId +" stored");
	    }

		Log.d(LOG_TAG,"DeviceId:" + deviceId);
		return deviceId;
	}

	public static String getVin() {
		String vin = readFromBackdoorFile(TEST_VIN);
		if (vin == null)
			return "TEST_VIN";
		return vin;
	}

	public static String getUserAgent(Context ctx) {
		return getDeviceId(ctx);
	}	
	
	public static String getDefaultValue(String Uri)
	{
		Log.d(LOG_TAG, "getDefaultValue URI: " + Uri);
		String res = null;
		if(Uri.equals("./Ext/RedBend/DmBootupMinDelay"))
		    res = "0s";
		else if(Uri.equals("./Ext/RedBend/UserInteractionTimeoutInterval"))
		    res = "1440";
		else if(Uri.equals("./Ext/RedBend/RecoveryPollingTimeout"))
		    res = "1440m";
		else if(Uri.equals("/Ext/Redbend/ExternalDownloadTimeout"))
		    res = "72h";		
		else if(Uri.equals("./Ext/RedBend/RecoveryPollingMaxCounter"))
		    res = "10";
		else if(Uri.equals("./Ext/RedBend/PostponePeriod"))
			res = "60";
		else if(Uri.equals("./Ext/RedBend/PostponeMaxTimes"))
			res = "3";
		else if(Uri.equals("./LAWMO/Ext/RedBend/Password/PwdFromServer"))
			res = "true";
		Log.d(LOG_TAG, "getDefaultValue, URI:"+ Uri + " value:" + res);
		return res;
	}
	
	private static String parseMac(String devID) {
		// If this is not mac address - use it 
		if (devID.length() < MAC_ADDRESS_LENGTH)
			return devID;
		// Remove from the mac address any ":" or "-" and pad with 0 
		String macDevId = devID.replaceAll(":|-", "");
		String ParsedDevId = macDevId + "000000000000000".substring(macDevId.length());
		return ParsedDevId;
	}

	private static String readFromBackdoorFile(String param){
		File file = new File(TEST_FILE_PATH);
		String value = null;
		// Read text from file
		if(file.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line;
				while ((line = br.readLine()) != null) {
					String[] params = line.split("=");
					String dv = params[0].toUpperCase(Locale.US);
					if ((dv.equals(param)) && (params.length > 1)) {
						value = params[1];
						Log.d(LOG_TAG, "Running in debug mode. " + param + " = " + value);
						break;
					}
				}
				br.close();
			} catch (IOException e) {
				Log.d(LOG_TAG, "Failed to parse test params");
			}
		}
		return value;
	}
	
	public static final String IPL_BEARER_WIFI = "Wifi";
	public static final String IPL_BEARER_TCU = "TCU";
	public static final String IPL_BEARER_BLUETOOTH = "Bluetooth";
	public static final String IPL_BEARER_UNKNOWN = "Unknown";

	public static String getBearer(Context ctx)
	{
	    String bearer = IPL_BEARER_UNKNOWN;
		File file = new File("/mnt/sdcard/bearer");
		if (file.exists()) {
			char[] inputBuffer = new char[1024];
			int q = 0;
			InputStream fIn = null;
			InputStreamReader isr = null;
			int BearerType = 0;
			
			try {
				fIn = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			isr = new InputStreamReader(fIn);

			try {
				q = isr.read(inputBuffer);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				isr.close();
				if (fIn != null) {
					fIn.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			BearerType = Integer.parseInt(String.valueOf(inputBuffer, 0, q).trim());
			switch (BearerType) {
				case 1:
					bearer = IPL_BEARER_TCU;
					break;
				case 2:
					bearer = IPL_BEARER_WIFI;
					break;
				case 3:
					bearer = IPL_BEARER_BLUETOOTH;
					break;
				default:
					bearer = IPL_BEARER_UNKNOWN;
					break;
		    }
		} else {
		    ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo netInfo = cm.getActiveNetworkInfo();

			switch (netInfo.getType()) {
				case ConnectivityManager.TYPE_MOBILE:
					bearer = IPL_BEARER_TCU;
					break;
				case ConnectivityManager.TYPE_WIFI:
				case ConnectivityManager.TYPE_ETHERNET:
					bearer = IPL_BEARER_WIFI;
					break;
				case ConnectivityManager.TYPE_BLUETOOTH:
					bearer = IPL_BEARER_BLUETOOTH;
					break;
				default:
					bearer = IPL_BEARER_UNKNOWN;
					break;
			}
		}

		Log.d(LOG_TAG, "[DEVINFO] Bearer: " + bearer);
		return bearer;
	}
	
}
