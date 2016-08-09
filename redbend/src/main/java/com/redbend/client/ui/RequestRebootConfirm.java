/*
 *******************************************************************************
 *
 * RequestRebootConfirm.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;

/**
 * Display a reboot to recovery mode dialog box. Reboots automatically if a silent update.
 */
public class RequestRebootConfirm extends DilActivity {
	
	private AlertDialog mDialog;

	private AlertDialog getDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.scomo_title))
		.setIcon(R.drawable.ic_menu_rb)
		.setTitle(getString(R.string.reboot_popup))
		.setCancelable(false)
		.setPositiveButton(getString(R.string.ok),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
			}
		});

		return builder.create();
	}

	@Override
	protected void setActiveView(boolean start, Event ev) {
		
		boolean isSilent = ev.getVarValue("DMA_VAR_SCOMO_ISSILENT") == 1;

		if (isSilent) {
			sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
			return;
		}

		if (mDialog == null)
			mDialog = getDialog();
		mDialog.show();
	}
	
}
