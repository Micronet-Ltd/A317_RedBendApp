/*
 *******************************************************************************
 *
 * RecoveryReboot.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
//MIcronet
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
 

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.content.Context;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.ClientService;
import com.redbend.client.R;
import com.redbend.client.ClientService.PRODUCT_TYPE;

/**
 * Display a reboot to recovery mode dialog box. Reboots automatically if a silent update.
 */
public class RecoveryReboot extends DilActivity {
	
	private String mGotaFile;
	private Thread mGotaThread;
	
	private AlertDialog mDialog;

	private final void reboot() {

		//Micronet
		String fnrk = "/runningkernel";
		String fncf = "/data/misc/rb/continueflag";
		String fnrf = "/data/misc/rb/recoveryflag";
		
		if (mGotaFile != null) {
			mGotaThread = new Thread(new Runnable() {
				public void run() {
					File updateFile = new File(getFilesDir().getAbsolutePath(), mGotaFile);
					
					try {
						RecoverySystem.verifyPackage(updateFile, null, null);
						RecoverySystem.installPackage(RecoveryReboot.this, updateFile);
						Log.d(LOG_TAG, "RecoveryReboot Performing GOTA update with file  '" +
								mGotaFile + "'");
					} catch (IOException e) {
						Log.d(LOG_TAG, "RecoveryReboot Request of GOTA update for file  '" + 
							mGotaFile + "' has failed");
						sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
					} catch (GeneralSecurityException e) {
						Log.d(LOG_TAG, "RecoveryReboot GOTA update file  '" +
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

		//Micronet
		//check that running kernel primary 
		File f = new File(fncf);
		if (f.exists()) {
		    File cfc = new File(fncf);
		    if (cfc.exists()) {
		        File frk = new File(fnrk);
		        if (frk.exists()) {
		            String Runningkernel;
		            try {
		                BufferedReader reader = new BufferedReader(new FileReader(fnrk), 256);
				try {
		                    Runningkernel = reader.readLine();
				} finally {
				    reader.close();
				}
		            } catch (IOException e) {
		                Log.e(LOG_TAG, "RecoveryReboot Exception reading running kernel file");
		                return;
		            }

			    if (Runningkernel.equals("primary") != true) {
			        Log.e(LOG_TAG, "RecoveryReboot Error: running [" + Runningkernel + "] or secondary kernel although continue flag exists");
			        return; //If continue flag exists primary kernel must be the one loaded
			    }
		        } else {
		            Log.e(LOG_TAG, "RecoveryReboot Error: no running kernel file");
			    return;//No way of knowing which kenrel is running
			}
		    }
		} else {
		    Log.d(LOG_TAG, "RecoveryReboot: no continue flag - which running kernel not checked");
		}
		//About to reset and run recovery. Create the recovery flag.
		File file = new File(fnrf);

		try {
		    file.createNewFile();
		} catch(Exception e) {
		    Log.e(LOG_TAG, "RecoveryReboot Error: failed to create recovery flag");
		    return;
		}
		if(file.exists()) {
		    try { 
			OutputStream fo = new FileOutputStream(file);
			fo.write(1);
			fo.close();
			Log.d(LOG_TAG, "RecoveryReboot: created recovery flag");
		    } catch (Exception e) {
			Log.e(LOG_TAG, "RecoveryReboot Error: failed to create output stream for recovery flag");
			return;
		    }
		} else {
		    Log.e(LOG_TAG, "RecoveryReboot Error: failed to create recovery flag - file doesn't exist");
		    return;
		}

		//remove continue flag before reset
		File fncfins = new File(fncf);
		if (fncfins.exists()) {
		    fncfins.delete();        	
		}
		//~Micronet		
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		try {
			pm.reboot("recovery");
		} catch (SecurityException e) {
			Log.d(LOG_TAG, "RecoveryReboot=>SecurityException " + e.toString());
			sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
		}

		finish();
	}

	private AlertDialog getDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.scomo_title))
		.setIcon(R.drawable.icon)
		.setTitle(getString(R.string.reboot_popup))
		.setCancelable(false)
		.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				reboot();
			}
		});

		return builder.create();
	}

	@Override
	protected void setActiveView(boolean start, Event ev) {
		
		if(ClientService.getProductType(this) == PRODUCT_TYPE.DOWNLOADABLE){
			Log.d(LOG_TAG, "RecoveryReboot PRODUCT_TYPE is DOWNLOADABLE, do not reboot");		
			sendEvent(new Event("DMA_MSG_SCOMO_INSTALL_CANNOT_PROCEED"));
			finish();
			return;
		}
		
		byte file[] = ev.getVarStrValue("DMA_VAR_SCOMO_UPDATE_CONFIG_FILE");
		boolean isSilent = ev.getVarValue("DMA_VAR_SCOMO_ISSILENT") == 1;

		if (file != null)
			mGotaFile = new String(file);
		else
			mGotaFile = null;

		Log.d(LOG_TAG, "RecoveryReboot called" + (isSilent ? " (silent)" : "") +
				", GOTA file: " + mGotaFile);		
		
			
		if (isSilent) {
			reboot();
			return;
		}

		if (mDialog == null)
			mDialog = getDialog();
		mDialog.show();
	}
	
}
