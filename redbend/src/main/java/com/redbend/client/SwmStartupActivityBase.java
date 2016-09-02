/*
 *******************************************************************************
 *
 * SwmStartupActivityBase.java
 *
 * Base class for StartupActivity and StartupActivityAutomotive.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.redbend.swm_common.DmcDeviceAdminReceiver;
import com.redbend.swm_common.ui.AdminUiBase;

import android.widget.Toast;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Process;

import com.redbend.client.R;

public abstract class SwmStartupActivityBase extends Activity {

    protected final static int REQUEST_CODE_ENABLE_ADMIN = 100;
    protected final String LOG_TAG = getClass().getSimpleName() + "(" + Integer.toHexString(hashCode()) + ")";

    protected abstract void sendStartServiceEvent();
    protected abstract void userAcceptedPermission();
    protected abstract void userDeclinedPermission();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!amIPrimaryUser())
        {
            Log.d(LOG_TAG, " Not primary user -- returning");
            String text = getString(R.string.not_primary_user);
            Log.d(LOG_TAG, text);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            finish();
        }



        if (amIAdmin()) {
            sendStartServiceEvent();
            finish();
        } else {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
                Log.d(LOG_TAG, " Starting Activity to add Admin");
                ComponentName adminName = new ComponentName(this, DmcDeviceAdminReceiver.class);
                AdminUiBase.startAdminPermissionActivity(this, adminName);
            } else {
                Log.d(LOG_TAG, " Skipping Activity to add Admin");
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "+onActivityResult");
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "onActivityResult=>userAcceptedPermission");
                userAcceptedPermission();
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(LOG_TAG, "onActivityResult=>userDeclinedPermission");
                userDeclinedPermission();
            }
        }
        finish();
        Log.d(LOG_TAG, "-onActivityResult");
    }
    
    private boolean amIPrimaryUser() {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN) {
            UserHandle uh = Process.myUserHandle();
            Context context = getApplicationContext();
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            long userSerialNumber = 0;
            if (null != um) {
                userSerialNumber = um.getSerialNumberForUser(uh);
                return userSerialNumber == 0;
            }
            return true;
        } else {
            Log.d(LOG_TAG, "SDK Build version " + Build.VERSION.SDK_INT + " is always primary user");
            return true; // always assume we are primary user
        }
    } // amIPrimaryUser()

    private boolean amIAdmin() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.d(LOG_TAG, "SDK Build version " + Build.VERSION.SDK_INT + " is always admin");
            return true;
        }

        Log.d(LOG_TAG, "Checking for Admin");

        DevicePolicyManager dPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (AdminUiBase.isAdmin(this, dPM)) {
            return true;
        }

        Log.d(LOG_TAG, "Not an admin");
        return false;
    } // amIAdmin()

}
