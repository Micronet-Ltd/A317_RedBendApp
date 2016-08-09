/*
 *******************************************************************************
 *
 * DpUtils.java
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
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import android.content.Context;
import android.util.Log;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.app.admin.DevicePolicyManager;

public class DpUtils {

	public final static String DP_COPY_POSTFIX = "_COPY";
	public final static String LOG_TAG = "DpUtils";
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

	public static void storeRbDpFile(File origDpFullName) throws IOException {
		copyFile(origDpFullName, new File(origDpFullName.getAbsolutePath() + DP_COPY_POSTFIX));
	}

	public static boolean restoreRbDpFile(File origDpFullName) {
		// delete file
		origDpFullName.delete();
		// move copy file to orig
		return new File(origDpFullName.getAbsolutePath() + DP_COPY_POSTFIX).renameTo(origDpFullName);
	}

	private static boolean getDeviceEncryptionStatus(Context ctx) {

		int status = DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED;
		boolean res = true;

		if (android.os.Build.VERSION.SDK_INT >= 11) {
			final DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
			if (dpm != null) {
				status = dpm.getStorageEncryptionStatus();
			}
		}

		if (status == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE ||
				status == DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED){
			res = false;
		}
		Log.d(LOG_TAG, "encrypt status is: " + status + " res is: " + res);

		return res;
	}

	public static boolean checkIfAndroidVerSupportsUncrypt() {
		// check if device support uncrypt, if not fail the session without a reboot
		boolean res = false;
		if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			return res = true;
		}
		return res;
	}

	public static boolean isEncryptHandle(Context ctx, File dpFullFileName) {
		boolean res = false;
		// handle encryption
		if (dpFullFileName != null) {
			if (getDeviceEncryptionStatus(ctx)) {
				// check if the DP exists in data
				if (dpFullFileName.getAbsolutePath().startsWith("/data")) {
					res = true;
				}
			}
		}
		return res;
	}
}
