/*
 *******************************************************************************
 *
 * GenericInstallerHandler.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.EventVar;
import com.redbend.client.micronet.MicronetFileUpload;
import com.redbend.client.micronet.MicronetLaunchApk;

import android.content.Context;
import android.util.Log;

public class GenericInstallerHandler extends EventHandler {
	public final static String TAG = "GenericInstallerHandler";

	
	public GenericInstallerHandler(Context ctx) {
		super(ctx);
	}

	@Override
	protected void genericHandler(Event ev) {
		Log.d(TAG, "+ GenericInstallerHandler receive events");
		if ("B2D_MSG_SCOMO_GENERIC_INSTALL_REQUEST".equals(ev.getName())) {
			Log.d(TAG, "receive B2D_MSG_SCOMO_GENERIC_INSTALL_REQUEST");
			GenericInstallDCInfo info = new GenericInstallDCInfo(ev);
			Log.d(TAG, info.toString());








			// MICRONET/DS 2017-08 : Start Add support for copying/removing files
			//int res = GenericInstallerIpl.installApk(info);
			int res = 0;
			if (MicronetFileUpload.checkIsCopyComponent(info.getDcId())) {
				// this component name indicates that we want to just copy or remove a file (it's not an APK)
				res = MicronetFileUpload.installOrRemoveFile(info.getDcId(), info.mode, info.getDpLocation(), info.getDcOffset(), info.getDcLength());
			} else
			if (MicronetLaunchApk.checkIsLaunchComponent(info.getDcId())) {
				MicronetLaunchApk.installOrRemoveApk(ctx, info.getDcId(), info.mode, info.getDpLocation(), info.getDcOffset(), info.getDcLength());
			} else {
				res = GenericInstallerIpl.installApk(info);
			}
			// MICRONET/DS 2017-08 : END Add support for copying/removing files




			Log.d(TAG, "Generic Installer IPL install result =" + res);
			Event event = new Event("D2B_MSG_SCOMO_INSTALL_RESULT")
					.addVar(new EventVar("DMA_VAR_SCOMO_INSTALL_COMP_RESULT",
							res));
			((ClientService) ctx).sendEvent(event);
		}
		Log.d(TAG, "- GenericInstallerHandler receive events" + ev.getName());
	}

	class GenericInstallDCInfo {
		int mode = -1;
		String dpLocation = null;
		String dcId = null;
		int dcOffset = 0;
		int dcLength = 0;
		int currType = 0;

		public GenericInstallDCInfo(Event ev) {
			mode = ev.getVarValue("DMA_VAR_SCOMO_MODE");

			byte[] d = ev.getVarStrValue("DMA_VAR_SCOMO_DP_LOCATION");
			if (d != null) {
				dpLocation = new String(d);
			}

			d = ev.getVarStrValue("DMA_VAR_SCOMO_DC_ID");
			if (d != null) {
				dcId = new String(d);
			}

			dcOffset = ev.getVarValue("DMA_VAR_SCOMO_DC_OFFSET");

			dcLength = ev.getVarValue("DMA_VAR_SCOMO_DC_LENGTH");

			currType = ev.getVarValue("DMA_VAR_SCOMO_DC_CURRTYPE");
		}

		public int getMode() {
			return mode;
		}

		public String getDpLocation() {
			return dpLocation;
		}

		public String getDcId() {
			return dcId;
		}

		public int getDcOffset() {
			return dcOffset;
		}

		public int getDcLength() {
			return dcLength;
		}

		public int getCurrType() {
			return currType;
		}

		public void setCurrType(int currType) {
			this.currType = currType;
		}

		@Override
		public String toString() {
			return "GenericInstallDCInfo [mode=" + mode + ", dpLocation="
					+ dpLocation + ", dcId=" + dcId + ", dcOffset=" + dcOffset
					+ ", dcLength=" + dcLength + ", currType=" + currType + "]";
		}
	}
}
