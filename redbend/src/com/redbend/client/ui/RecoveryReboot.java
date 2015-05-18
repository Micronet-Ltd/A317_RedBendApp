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
