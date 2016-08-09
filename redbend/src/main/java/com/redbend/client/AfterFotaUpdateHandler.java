/*
 *******************************************************************************
 *
 * AfterFotaUpdateHandler.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.client.DpUtils;

import android.content.Context;
import android.util.Log;
import java.io.File;

public class AfterFotaUpdateHandler extends EventHandler {
	public final static String TAG = "AfterFotaUpdateHandler";

	
	public AfterFotaUpdateHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected void genericHandler(Event ev) {
		Log.d(TAG, "+ AfterFotaUpdateHandler receive events");
		if ("B2D_CONTINUE_UPDATE_AFTER_FOTA".equals(ev.getName())) {
			Log.d(TAG, "receive B2D_CONTINUE_UPDATE_AFTER_FOTA");
			// get dp full name
			byte byteDpFullName[] = ev.getVarStrValue("DMA_VAR_SCOMO_DP_FULL_NAME");
			if (byteDpFullName != null) {
				String dpFullFileName = new String (byteDpFullName); 
				File fDpFullFileName = new File(dpFullFileName);
				
				boolean isMoveFileNeeded = DpUtils.isEncryptHandle(ctx, fDpFullFileName);
				Log.d(TAG, "AfterFotaUpdateHandler should handle uncrypt dp: " + isMoveFileNeeded);
				if (isMoveFileNeeded == true) {
					//getting here, means that our DP is decrypted and needs to be restored
					if (DpUtils.restoreRbDpFile(fDpFullFileName) == true)
						Log.e(TAG, "Dp restored successfully");
					else
						Log.e(TAG, "Dp failed to be restored " + fDpFullFileName.getAbsolutePath());
				}
			}
		}
		// accept is sent in any case to continue the regular flow
		((ClientService) ctx).sendEvent(new Event("DMA_MSG_SCOMO_ACCEPT"));
		Log.d(TAG, "-AfterFotaUpdateHandler receive events" + ev.getName());
	}

}
