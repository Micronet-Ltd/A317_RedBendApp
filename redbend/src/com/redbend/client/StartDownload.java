/*
 *******************************************************************************
 *
 * StartDownload.java
 *
 * Moves download to background if not in user mode (checks DMA_VAR_SCOMO_TRIGGER_MODE).
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_INIT (foreground only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.EventVar;

/** 
 * Start a download if not in user mode.
 */
public class StartDownload extends EventHandler {

	private final String LOGTAG = getClass().getSimpleName();

	public StartDownload(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected void genericHandler(Event ev) {
		EventVar mode;
		
		try {
			mode = ev.getVar("DMA_VAR_SCOMO_TRIGGER_MODE");
			if (mode.getValue() == SmmConstants.SCOMO_MODE_USER)
				return;
			
			Log.i(LOGTAG, "DownloadProgress received event 0x" + 
					ev.getName() + ", when not SCOMO_MODE_USER: sending task to back");
			((ClientService) ctx).startFlowInBackground(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
