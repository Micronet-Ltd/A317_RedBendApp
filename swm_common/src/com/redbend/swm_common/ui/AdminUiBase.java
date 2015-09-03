/*
 *******************************************************************************
 *
 * AdminUiBase.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.Manifest;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmService;
import com.redbend.swm_common.DmcDeviceAdminReceiver;
import com.redbend.swm_common.SmmCommonConstants;

/**
 * Perform admin operations.
 */
public abstract class AdminUiBase extends DilActivity {
	private final static int REQUEST_CODE_ENABLE_ADMIN = 100;
    protected DevicePolicyManager m_dpm;
	protected ComponentName m_adminName;
	protected Event m_ev;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_dpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		m_adminName = new ComponentName(this, DmcDeviceAdminReceiver.class);
	}

	private void startAdminPermissionActivity() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, m_adminName);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
			"SWM client requests device policy");
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	}

	abstract protected int performOperation();

	private void sendResultEvent(int result){
		Event ev = new Event("MSG_DESCMO_RESULT");
		ev.addVar(new EventVar("VAR_DESCMO_RESULT", result));
		sendEvent(ev);
	}

	@Override
	protected void setActiveView(boolean start, Event ev) {
		// Must be implemented, but we don't need to display anything
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
			int result;
			
			if (resultCode == RESULT_OK) {
				Log.d(LOG_TAG, "onActivityResult: user accepted admin permissions request");
				result = performOperation();
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(LOG_TAG, "onActivityResult: user declined admin permissions request");
				result = SmmCommonConstants.DESCMO_OPERATION_CANCELED;
			} else
				result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
		
			sendResultEvent(result);
		}
		finish();
	}

	@Override
	protected void newEvent(Event receivedEvent) {
		super.newEvent(receivedEvent);
		String eventName = receivedEvent.getName();
		if (eventName.equals("MSG_DESCMO_USER_INTERACTION_TIMEOUT")) {
			// finish activity causes onActivityResult, which finishes our activity
			finishActivity(REQUEST_CODE_ENABLE_ADMIN);
		}
		else {
			m_ev = new Event(receivedEvent);
			if (enableAdmin(this, m_dpm, false)) {
				Log.d(LOG_TAG, "newEvent: application already has Admin permissions");
				int result = performOperation();
				sendResultEvent(result);
				finish();
			}
			else
				startAdminPermissionActivity();
		}
	}

	@SuppressWarnings("deprecation")
	private static boolean isDeviceProvisioned(Context ctx) {
		return Settings.Secure.getInt(ctx.getContentResolver(),
			Settings.Secure.DEVICE_PROVISIONED, 0) > 0;
	}

	public static boolean enableAdmin(Context ctx, DevicePolicyManager dpm, boolean setOwner) {
		final String LOG_TAG = "enableAdmin";
		ComponentName cn = new ComponentName(ctx.getApplicationContext(), DmcDeviceAdminReceiver.class);
		boolean success = false;

		if (dpm.isAdminActive(cn))
			return true;

		if (!SmmService.isPermissionGranted(Manifest.permission.BIND_DEVICE_ADMIN, ctx)) {
			Log.i(LOG_TAG, "Doesn't have permissions to set Administrator rights");
			return false;
		}
		Log.i(LOG_TAG, "Setting Administrator rights");

		try {
			Method setActiveAdmin = DevicePolicyManager.class.getMethod
					("setActiveAdmin", ComponentName.class, boolean.class);
			setActiveAdmin.invoke(dpm, cn, false);
			success = true;
		} catch (NoSuchMethodException e) {
			Log.w(LOG_TAG, "Cannot enforce administrator rights", e);
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Error setting Admin rights", e);
		} catch (IllegalAccessException e) {
			Log.e(LOG_TAG, "Error setting Admin rights", e);
		} catch (InvocationTargetException e) {
			Log.e(LOG_TAG, "Error setting Admin rights", e);
		}
		if (!success)
			return false;
		if (!setOwner)
			return true;

		if (isDeviceProvisioned(ctx)) {
			Log.i(LOG_TAG, "Device already provisioned, cannot set device owner");
			return true;
		}
		try {
			Log.i(LOG_TAG, "Setting As Device Owner");
			Method setDeviceOwner = DevicePolicyManager.class.getMethod
					("setDeviceOwner", String.class);
			success = (Boolean) setDeviceOwner.invoke(dpm, ctx.getPackageName());
			if (!success)
				Log.w(LOG_TAG, "Couldn't set as device owner");
		} catch (NoSuchMethodException e) {
			Log.i(LOG_TAG, "No setDeviceOwner method, skipping");
		} catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Error setting device owner", e);
		} catch (IllegalAccessException e) {
			Log.e(LOG_TAG, "Error setting device owner", e);
		} catch (InvocationTargetException e) {
			Log.e(LOG_TAG, "Error setting device owner", e);
		}
		return true;
	}
}
