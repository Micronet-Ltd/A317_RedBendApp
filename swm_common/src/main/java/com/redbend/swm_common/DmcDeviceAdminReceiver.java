/*
 *******************************************************************************
 *
 * DmcDeviceAdminReceiver.java
 *
 * Required for the DM operations related to LAWMO factory reset and lock
 * operations.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* Required for the DM operations related to LAWMO factory reset, lock
 * operations, DESCMO.  
 */
public class DmcDeviceAdminReceiver extends DeviceAdminReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("DmcDeviceAdminReceiver", "onReceive()");
	}
}
