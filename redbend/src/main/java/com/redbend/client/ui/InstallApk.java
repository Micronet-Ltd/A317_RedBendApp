/*
 *******************************************************************************
 *
 * InstallApk.java
 *
 * Manages APK installation / uninstallation.
 *
 * Receives DIL events:
 * DMA_MSG_SCOMO_INSTALL_COMP_REQUEST
 * DMA_MSG_SCOMO_REMOVE_COMP_REQUEST
 * DMA_MSG_SCOMO_CANCEL_COMP_REQUEST
 * 
 * Sends BL events:
 * DMA_MSG_SCOMO_INSTALL_COMP_RESULT
 * DMA_MSG_SCOMO_REMOVE_COMP_RESULT
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.EventVar;
import com.redbend.client.ClientService;
import com.redbend.client.micronet.MicronetFileUpload;

public class InstallApk extends EventHandler {

	public InstallApk(Context ctx) {
		super(ctx);
	}

	static private final String INSTALL_EVENT = "DMA_MSG_SCOMO_INSTALL_COMP_REQUEST";
	static private final String REMOVE_EVENT = "DMA_MSG_SCOMO_REMOVE_COMP_REQUEST";
	static private final String CANCEL_EVENT = "DMA_MSG_SCOMO_CANCEL_COMP_REQUEST";
	static private final String FAILURE = "Failure";
	
	static private final String PM_EXEC = "pm";

	/* values taken from swm_general_errors.h */
	private static final int SWM_UA_ERR_FAILED_TO_UNINSTALL_APK = 0x0200;
	private static final int SWM_UA_ERR_FAILED_TO_INSTALL_APK = 0x0201;

	private static final String LOG_TAG = "InstallApk";
	
	private void sendResult(boolean install, int ret) {
		String event = install ? "DMA_MSG_SCOMO_INSTALL_COMP_RESULT"
				: "DMA_MSG_SCOMO_REMOVE_COMP_RESULT";
		String varName = install ? "DMA_VAR_SCOMO_INSTALL_COMP_RESULT"
				: "DMA_VAR_SCOMO_REMOVE_COMP_RESULT";
		((ClientService)ctx).sendEvent(new Event(event).addVar(new EventVar(varName, ret)));
	}
	
	/*
	 * Installs a given APK file
	 */
	private static int installRoot(String apkFile, boolean useRootPermission) {
		int res;

		Log.i(LOG_TAG, "installRoot: use_root_permissions = "
				+ useRootPermission);

		if (useRootPermission)
			res = runProcess("su", "-c",
					String.format("%s install -r %s", PM_EXEC, apkFile));
		else
			res = runProcess(PM_EXEC, "install", "-r", apkFile);

		if (res != 0)
			res = SWM_UA_ERR_FAILED_TO_INSTALL_APK;
		Log.i(LOG_TAG, "installRoot: result: 0x" + Integer.toHexString(res));
		return res;
	}
	
	/*
	 * Installs a given APK file
	 */
	private static int uninstallRoot(String compName, boolean useRootPermission) {
		int res;

		Log.i(LOG_TAG, "uninstallRoot: use_root_permissions = "
				+ useRootPermission);

		if (useRootPermission)
			res = runProcess("su", "-c",
					String.format("%s uninstall %s", PM_EXEC, compName));
		else
			res = runProcess(PM_EXEC, "uninstall", compName);

		if (res != 0)
			res = SWM_UA_ERR_FAILED_TO_UNINSTALL_APK;
		Log.i(LOG_TAG, "uninstallRoot: result: 0x" + Integer.toHexString(res));
		return res;
	}
	
	public void exec(Event event) {
		final String eventName = event.getName();

		if (INSTALL_EVENT.equals(eventName)) {
			String apkFile = new String(
					event.getVarStrValue("DMA_VAR_SCOMO_COMP_FILE"));

			// Micronet changes
			int res;
			if (MicronetFileUpload.checkIsApkCopyFile(apkFile)) {
				// if the file has the correct name, then we want to just copy it instead of install it
				boolean bres = MicronetFileUpload.copyApkFile(apkFile);

				res = (bres ? 0 : SWM_UA_ERR_FAILED_TO_INSTALL_APK);

			} else {
			// END Micronet changes
				res = installRoot(apkFile, false);
			}

			sendResult(true, res);
		} else if (REMOVE_EVENT.equals(eventName)) {
			String compName = new String(
					event.getVarStrValue("DMA_VAR_SCOMO_COMP_NAME"));

			sendResult(false, uninstallRoot(compName, false));
			
		} else {
			Log.e(LOG_TAG, "Unkown event: " + eventName);
		}
	}

	@Override
	protected void genericHandler(Event ev) {

		if (CANCEL_EVENT.equals(ev.getName()) ) {
			Log.i(LOG_TAG,
					"Requested to cancel the installation, finishing the activity");
			return;
		} 

		exec(ev);
	}
	
	private static int runProcess(String... cmd) {
		int ret = 0;
		StringBuilder command = new StringBuilder();

		for (String c : cmd) {
			command.append(' ');
			command.append(c);
		}
		command.deleteCharAt(0);

		try {
			Process process = new ProcessBuilder().command(cmd)
					.redirectErrorStream(true).start();

			Reader reader = new InputStreamReader(process.getInputStream());
			int chr;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((chr = reader.read()) != -1)
				out.write(chr);
			String str = out.toString("UTF-8");
			out.close();
			reader.close();
			Log.d(LOG_TAG, "runProcess buffer: " + str);
			ret = str.contains(FAILURE) ? -1 : 0;

			Log.i(LOG_TAG, "Finished executing '" + command + "', ret=" + ret);
			process.destroy();
				
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
}
