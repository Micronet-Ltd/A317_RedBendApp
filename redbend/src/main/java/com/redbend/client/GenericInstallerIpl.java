/*
 *******************************************************************************
 *
 * GenericInstallerIpl.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.File;

import com.redbend.SwmcInstallerHelper;
import com.redbend.ComponentInfo;
import com.redbend.client.GenericInstallerHandler.GenericInstallDCInfo;
import com.redbend.client.micronet.MicronetFileUpload;
import com.redbend.client.micronet.MicronetLaunchApk;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class GenericInstallerIpl {

	public final static String TAG = "GenericInstallerIpl";

	private SwmcInstallerHelper m_helper = null;
	public static String GenericInstallerProp = "com.redbend.client.gi";
	private static boolean m_giEnabled;
	public GenericInstallerIpl(Context ctx) {
		m_helper = new SwmcInstallerHelper(ctx);
		Resources res = ctx.getResources();
		m_giEnabled= res.getBoolean(R.bool.isGenericInstallerSimEnabled);
	}

	public String getNextComponent(int type, int[] iter) {
		if (!m_giEnabled)
			return null;
		Log.d(TAG, "getNextComponent type=" + type + " index=" + iter[0]);
		return m_helper.getNextComponent(iter);
	}

	public ComponentInfo getComponentAttribute(int type, String componentId) {
		if (!m_giEnabled)
			return null;
	    Log.d(TAG, "getComponentAttribute type=" + type + "componentId=" + componentId);

		// Micronet/DS 2017-08
		if (type==200) {
			if (MicronetFileUpload.checkIsCopyComponent(componentId)) {
				return MicronetFileUpload.getComponentInfo(componentId);
			} else if (MicronetLaunchApk.checkIsLaunchComponent(componentId)) {
				return m_helper.getComponentAttribute(MicronetLaunchApk.getRealPackageName(componentId));
			}

		}
		// Micronet/DS End
		return m_helper.getComponentAttribute(componentId);
	}

	public static int installApk(GenericInstallDCInfo info) {
		String dcPath = getDCfile(info);
		int ret = 0;
		Log.d(TAG, "info.mode=" + info.mode);
		switch (info.mode) {
		case 1: // install
		case 2: // update
			if (!m_giEnabled) return 0x0201; //SWM_UA_ERR_FAILED_TO_INSTALL_APK
			ret = runProcess("pm", "install", "-r", dcPath) ? 0 : 0x0201;
			Log.d(TAG, "install apk = " + ret);
			break;
		case 3: // remove
			if (!m_giEnabled) return 0x0200; //SWM_UA_ERR_FAILED_TO_UNINSTALL_APK
			ret = runProcess("pm", "uninstall", info.dcId) ? 0 : 0x0200;
			Log.d(TAG, "install apk = " + ret);
			break;
		}
		
		if (dcPath != null)
		{
			File dcFile = new File(dcPath);
			if (dcFile.exists())
				dcFile.delete();
		}
		
		return ret;
	}

	private static String getDCfile(GenericInstallDCInfo info) {
		int readBytes = 0;
		int bufferSize = 8192;
		byte[] buf = new byte[bufferSize];
		int bytesToCopy = info.dcLength;
		int index = info.dpLocation.lastIndexOf("/");
		String dcPath = info.dpLocation.substring(0, index) + "/" + info.dcId;

		Log.d(TAG, "DC path = " + dcPath + "DC length= " + bytesToCopy
				+ " bufferSize=" + bufferSize);

		try {
			RandomAccessFile fin = new RandomAccessFile(info.dpLocation, "r");
			RandomAccessFile fout = new RandomAccessFile(dcPath, "rw");
			fin.seek(info.dcOffset);

			//Start copy component from DP into the target file
			while (bytesToCopy > 0) {
				readBytes = fin.read(buf);
				if (readBytes != -1) {
					fout.write(buf,0,readBytes) ;
					bytesToCopy -= readBytes;
				} else
					Log.e(TAG, "unexpected end of file");
			}
			fin.close();
			fout.close();
			File file = new File(dcPath);
			file.setReadable(true, false);
			file.setExecutable(true, false);

		} catch (IOException ioE) {
			Log.d(TAG, Log.getStackTraceString(ioE));
			return null;
		}

		Log.d(TAG, "-getDCfile ");
		return dcPath;
	}

	private static boolean runProcess(String... cmd) {
		boolean ret = true;
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
			Log.d(TAG, "runProcess buffer: " + str);
			ret = str.contains("Failure") ? false : true;

			Log.i(TAG, "Finished executing '" + command + "', ret=" + ret);
			process.destroy();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return ret;
	}
}
