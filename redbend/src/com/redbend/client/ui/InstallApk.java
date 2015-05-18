/*
 *******************************************************************************
 *
 * InstallApk.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.client.R;

public class InstallApk extends DilActivity {

	private PackageManager pm;
	private ProgressDialog mProgressDialog;

	static private final String INSTALL_EVENT = "DMA_MSG_SCOMO_INSTALL_COMP_REQUEST";
	static private final String REMOVE_EVENT = "DMA_MSG_SCOMO_REMOVE_COMP_REQUEST";
	static private final String CANCEL_EVENT = "DMA_MSG_SCOMO_CANCEL_COMP_REQUEST";
	static private final String FAILURE = "Failure";
	
	static private final String PM_EXEC = "pm";

	private static final int INSTALL_REQUEST = 1;
	private static final int UNINSTALL_REQUEST = 2;

	/* values taken from swm_general_errors.h */
	private static final int SWM_UA_ERR_FAILED_TO_UNINSTALL_APK = 0x0200;
	private static final int SWM_UA_ERR_FAILED_TO_INSTALL_APK = 0x0201;

	/* taken from Intent, since there it's hidden */
	private static final String EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT";

	private ProcessEvent mProcessEvent;
	private boolean mNeedSecondRun;
	private BroadcastReceiver mPackageReceiver;

	private class PackageReceiver extends BroadcastReceiver {
		private final String mPkgName;
		private final boolean mInstall;

		public PackageReceiver(boolean install, String pkgName) {
			mPkgName = pkgName;
			mInstall = install;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Uri data = intent.getData();
			String pkgName = data != null ? data.getSchemeSpecificPart() : null;

			Log.i(LOG_TAG, String.format(
					"Action: %s, Package expected: %s, Package received: %s",
					intent.getAction(), mPkgName, pkgName));
			if (mPkgName.equals(pkgName)) {
				InstallApk.this.finishActivity(mInstall? INSTALL_REQUEST : UNINSTALL_REQUEST);
				sendResult(mInstall, 0);
				Log.i(LOG_TAG, "finishing activity: " + mInstall);
			}
		}
	}

	private void registerInstallReceiver(String apkPath) {
		PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, 0);

		mPackageReceiver = new PackageReceiver(true, pkgInfo.packageName);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		intentFilter.addDataScheme("package");

		registerReceiver(mPackageReceiver, intentFilter);
	}

	private void registerUninstallReceiver(String pkgName) {
		mPackageReceiver = new PackageReceiver(false, pkgName);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addDataScheme("package");

		registerReceiver(mPackageReceiver, intentFilter);
	}

	/**
	 * Get a flag indicating whether install permission was granted.
	 * 
	 * @return true if install permission was granted, false otherwise
	 * 
	 */
	private boolean isInstallPermissionGranted() {
		boolean isInstallPermissionGranted = false;

		isInstallPermissionGranted = pm.checkPermission(
				Manifest.permission.INSTALL_PACKAGES, getPackageName()) == PackageManager.PERMISSION_GRANTED;

		Log.d(LOG_TAG, "isInstallPermissionGranted = "
				+ isInstallPermissionGranted);
		return isInstallPermissionGranted;
	}

	/**
	 * Install apk.
	 */
	private void installNonRoot(String apkFile, int requestCode, boolean needRegister) {
		Log.d(LOG_TAG, String.format("install apkFile: %s", apkFile));

		Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
		intent.setDataAndType(Uri.parse("file://" + apkFile),
				"application/vnd.android.package-archive");

		if (needRegister)
			registerInstallReceiver(apkFile);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * Uninstall apk.
	 */
	private void uninstallNonRoot(String compId, int requestCode) {
		Log.d(LOG_TAG, String.format("uninstall compId: %s", compId));

		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(Uri.fromParts("package", compId, null));

		registerUninstallReceiver(compId);
		startActivityForResult(intent, requestCode);
	}

	private int runProcess(String... cmd) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	/*
	 * Installs a given APK file
	 */
	private int installRoot(String apkFile, boolean useRootPermission) {
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
	private int uninstallRoot(String compName, boolean useRootPermission) {
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

	private boolean canInstallNonMarketApps() {
		boolean unknownSource = false;
		if (Build.VERSION.SDK_INT < 3) {
		    unknownSource = Settings.System.getInt(getContentResolver(), Settings.System.INSTALL_NON_MARKET_APPS, 0) == 1;
		}
		else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
		    unknownSource = Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
		} else {
		    unknownSource = Settings.Global.getInt(getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1;
		}
		return unknownSource;
	}

	static boolean mResultAlreadySent;

	private synchronized void sendResult(boolean install, int ret) {
		/*
		 * avoid sending result more than once, for example after the
		 * PackageReceiver calls it, onActivityResult could call it again
		 */
		if (mResultAlreadySent) {
			Log.i(LOG_TAG, "Result has been sent already");
			return;
		}
		String event = install ? "DMA_MSG_SCOMO_INSTALL_COMP_RESULT"
				: "DMA_MSG_SCOMO_REMOVE_COMP_RESULT";
		String varName = install ? "DMA_VAR_SCOMO_INSTALL_COMP_RESULT"
				: "DMA_VAR_SCOMO_REMOVE_COMP_RESULT";
		sendEvent(new Event(event).addVar(new EventVar(varName, ret)));
		mResultAlreadySent = true;
	}

	private class ProcessEvent implements Runnable {

		private final Event mEvent;
		private int mRequestCode;

		public ProcessEvent(Event event) {
			mEvent = event;
			mRequestCode = -1;
		}

		public void finishStartedActivity() {
			if (mRequestCode != -1)
				finishActivity(mRequestCode);
		}

		public void run() {
			final String eventName = mEvent.getName();

			if (INSTALL_EVENT.equals(eventName)) {
				String apkFile = new String(
						mEvent.getVarStrValue("DMA_VAR_SCOMO_COMP_FILE"));

				if (isInstallPermissionGranted()) {
					sendResult(true, installRoot(apkFile, false));
				} else {
					boolean needRegister;
					
					/*
					 * non root starts a new activity, so the result event is
					 * sent, when this activity ends and onActivityResult is
					 * called with negative result, or the new application has
					 * been installed
					 */
					if (!mNeedSecondRun) {
						mNeedSecondRun = !canInstallNonMarketApps();
						needRegister = true;
					}
					else {
						mNeedSecondRun = false;
						needRegister = false;
					}
					mRequestCode = INSTALL_REQUEST;
					installNonRoot(apkFile, mRequestCode, needRegister);
				}
			} else if (REMOVE_EVENT.equals(eventName)) {
				String compName = new String(
						mEvent.getVarStrValue("DMA_VAR_SCOMO_COMP_NAME"));

				if (isInstallPermissionGranted()) {
					sendResult(false, uninstallRoot(compName, false));
				} else {
					/*
					 * non root starts a new activity, so the result event is
					 * sent, when this activity ends, and onActivityResult is
					 * called with negative result, or the application has been
					 * uninstalled
					 */
					mRequestCode = UNINSTALL_REQUEST;
					uninstallNonRoot(compName, mRequestCode);
				}
			} else {
				Log.e(LOG_TAG, "Unkown event: " + eventName);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		int pmResult = data != null ? data.getIntExtra(EXTRA_INSTALL_RESULT, 0)
				: 0;

		Log.i(LOG_TAG, "+onActivityResult");
		/*
		 * since resultCode is not updated to OK, we need other way to check
		 * whether the operation has succeeded, so we have PackageReceiver that
		 * checks the success
		 */
		switch (requestCode) {
		case INSTALL_REQUEST:
			if (mNeedSecondRun) {
				if (canInstallNonMarketApps()) {
					Log.i(LOG_TAG,
							"Install has requested to confirm \"Unknown Sources\", "
									+ "performing second try");
					new Thread(mProcessEvent).start();
					return;
				}
				Log.w(LOG_TAG,
						"User didn't confirm \"Unknown Sources\", aborting");
			}
			Log.i(LOG_TAG, "Install finished with code: " + resultCode
					+ " pm result: " + pmResult);
			sendResult(true, SWM_UA_ERR_FAILED_TO_INSTALL_APK);
			break;
		case UNINSTALL_REQUEST:
			Log.i(LOG_TAG, "Uninstall finished with code: " + resultCode
					+ " pm result: " + pmResult);
			sendResult(false, SWM_UA_ERR_FAILED_TO_UNINSTALL_APK);
			break;
		default:
			Log.e(LOG_TAG, "Invalid requestCode received: " + requestCode);
			finish();
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pm = getPackageManager();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mPackageReceiver != null)
			unregisterReceiver(mPackageReceiver);
		mPackageReceiver = null;
		mProcessEvent = null;
	}

	@Override
	protected void setActiveView(boolean start, Event receivedEvent) {
		boolean isSilent = false;
		try {
			EventVar isSilentVar = receivedEvent.getVar("DMA_VAR_SCOMO_ISSILENT");
			if (isSilentVar != null)
				isSilent = (isSilentVar.getValue()==1); 
		} catch (Exception e) {
			Log.e(LOG_TAG, e.toString());
		}
	
		if (!isSilent)
			mProgressDialog = ProgressDialog.show(this, "", getResources()
				.getString(R.string.update_in_progress_dialog), true);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mProgressDialog != null)
			mProgressDialog.cancel();
	}

	@Override
	protected void newEvent(Event receivedEvent) {
		super.newEvent(receivedEvent);

		/* make sure we clear previous unfinished requests */
		if (mProcessEvent != null) {
			Log.w(LOG_TAG, "Closing previous non-confirmed window");
			mProcessEvent.finishStartedActivity();
			if (mPackageReceiver != null) {
				unregisterReceiver(mPackageReceiver);
				mPackageReceiver = null;
			}
		}

		mNeedSecondRun = false;
		mResultAlreadySent = false;

		if (CANCEL_EVENT.equals(receivedEvent.getName()) ) {
			Log.i(LOG_TAG,
					"Requested to cancel the installation, finishing the activity");
			finish();
			return;
		} else if ("DMA_MSG_SCOMO_INSTALL_DONE".equals(receivedEvent.getName())){
			Log.i(LOG_TAG,
					"Install done, finishing the activity");
			finish();
			return;
		}

		mProcessEvent = new ProcessEvent(receivedEvent);
		new Thread(mProcessEvent).start();
	}
}
