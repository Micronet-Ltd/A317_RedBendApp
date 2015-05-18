/*
 *******************************************************************************
 *
 * SwmStartupActivityBase.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.redbend.swm_common.DmcDeviceAdminReceiver;

public abstract class SwmStartupActivityBase extends Activity {
	
	protected final static int REQUEST_CODE_ENABLE_ADMIN = 100;
	protected final String LOG_TAG = getClass().getSimpleName() + "(" + Integer.toHexString(hashCode()) + ")";
	
	protected abstract void sendStartServiceEvent();
	protected abstract void userAcceptedPermission();
	protected abstract void userDeclinedPermission();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DevicePolicyManager dPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName adminName = new ComponentName(this, DmcDeviceAdminReceiver.class);		
		
		if(dPM.isAdminActive(adminName)){
			//If has permission start enrollment
			sendStartServiceEvent();
			finish();
		}else{
			startAdminPermissionDialog(adminName);
		}
	}
	
	public void startAdminPermissionDialog(ComponentName adminName) {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
				"SWM client requests device policy");
		startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e(LOG_TAG, "+onActivityResult");
		if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
			if (resultCode == RESULT_OK) {
				Log.e(LOG_TAG, "onActivityResult=>userAcceptedPermission");
				userAcceptedPermission();
			} else if (resultCode == RESULT_CANCELED) {
				Log.e(LOG_TAG, "onActivityResult=>userDeclinedPermission");
				userDeclinedPermission();
			}
		}
		finish();
		Log.e(LOG_TAG, "-onActivityResult");
	}

}
