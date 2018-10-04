/*
 *******************************************************************************
 *
 * RequestReboot.java
 *
 * Reboots to recovery mode.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_REBOOT_REQUEST
 *
 * Sends BL events:
 * DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED
 * DMA_MSG_SCOMO_VERIFICATION_FAILURE
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import android.util.Log;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.content.Context;
import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.ClientService;
import com.redbend.client.ClientService.PRODUCT_TYPE;
import com.redbend.client.micronet.MicronetReboot;

/**
 * Display a reboot to recovery mode dialog box. Reboots automatically if a silent update.
 */
public class RequestReboot extends DilActivity {
	
	private Thread mGotaThread;
	private File mGotaFile;
	public final static String DP_TEMP_POSTFIX = "_COPY";
	public static final int BYTE_ARRAY_SIZE = 1024;

	private static void copyFile(File src, File dst) throws IOException{
		InputStream in = new FileInputStream(src); 
		OutputStream out = new FileOutputStream(dst); 
		byte[] buffer = new byte[BYTE_ARRAY_SIZE];
		int count;

		while ((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);
		out.close();
		in.close();
	}
	
	private final void reboot() {
		if (mGotaFile != null) {
			mGotaThread = new Thread(new Runnable() {
				public void run() {
					if (!mGotaFile.exists())
					{// means that the rb OTA file is not in /data/data/<pkg name>/files
					 // so we need to copy it to the new location
						try {
							copyFile(new File(getFilesDir(), mGotaFile.getName()), mGotaFile);
						} catch (IOException e1) {
							sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
							runOnUiThread(new Runnable() {
								public void run() {
									finish();
								}
							});
							return;
						}
					}
					try {
						RecoverySystem.verifyPackage(mGotaFile, null, null);
						RecoverySystem.installPackage(RequestReboot.this, mGotaFile);
						Log.d(LOG_TAG, "RequestReboot Performing GOTA update with file  '" +
								mGotaFile + "'");
					} catch (IOException e) {
						Log.d(LOG_TAG, "RequestReboot Request of GOTA update for file  '" + 
								mGotaFile + "' has failed");
						sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
					} catch (GeneralSecurityException e) {
						Log.d(LOG_TAG, "RequestReboot GOTA update file  '" +
								mGotaFile + "' verification has failed");
						sendEvent(new Event("DMA_MSG_SCOMO_VERIFICATION_FAILURE"));
					}
					runOnUiThread(new Runnable() {
						public void run() {
							finish();
						}
					});
				}
			});
			mGotaThread.start();

			return;
		}

		MicronetReboot.prepareRebootFlags();


		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		try {
			pm.reboot("recovery");
		} catch (SecurityException e) {
			Log.d(LOG_TAG, "RequestReboot=>SecurityException " + e.toString());
			sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
		}

		finish();
	}

	
	@Override
	protected void setActiveView(boolean start, Event ev) {
		
		if(ClientService.getProductType(this) != PRODUCT_TYPE.SYSTEM){
			Log.d(LOG_TAG, "RequestReboot PRODUCT_TYPE is DOWNLOADABLE, do not reboot");		
			sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
			finish();
			return;
		}
		
		String eventName = ev.getName();
		// if a regular boot request
		if (eventName.equals("B2D_REBOOT_TO_BOOT_PARTITION")) 
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			try {
				pm.reboot("boot");
			} catch (SecurityException e) {
				Log.e(LOG_TAG, "RequestReboot=>SecurityException " + e.toString());
				sendEvent(new Event("D2B_REBOOT_FAILURE"));
			}
			finish();
			return;
		}
		
		mGotaFile = getLocalDropInFile(ev);
		if (mGotaFile == null)
			//if the ota zip file was not stored at integration, it might been 
			// download from server 
			mGotaFile = getRemoteDropInFile(ev);
		
		Log.d(LOG_TAG, "RequestReboot called" + ", GOTA file: " + mGotaFile);		
		reboot();
	}
	
	private File getLocalDropInFile(Event ev){
		//ota zip file was placed in the device at integration time 
		byte file[] = ev.getVarStrValue("DMA_VAR_SCOMO_UPDATE_CONFIG_FILE");
		byte path[] = ev.getVarStrValue("DMA_VAR_SCOMO_UPDATE_CONFIG_PATH");

		if (file == null)
		{
			//if the ota zip file was not stored at integration, it might been 
			// download from server 
			Log.d(LOG_TAG, "This is not local drop in integration");
			return null;
		}

		String gotaFile = new String(file);
		String gotaPath = null;
		if(path != null)
			gotaPath =  new String(path);
		else
			gotaPath = getFilesDir().getAbsolutePath();
		return new File(gotaPath, gotaFile);
	}
	
	private File getRemoteDropInFile(Event ev){
		byte fileByteArr[] = ev.getVarStrValue("DMA_VAR_DP_INSTALLER_PATHS_ARRAY");
		if (fileByteArr == null){
			Log.d(LOG_TAG, "This is not remote drop in integration");
			return null;
		}
		String filePath = new String(fileByteArr).split(";")[0];
		return new File(filePath);
	}
	
}

