/*
 *******************************************************************************
 *
 * AdminUiBase.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.swm_common.DmcDeviceAdminReceiver;
import com.redbend.swm_common.R;
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

    protected abstract int performOperation(Event event);

    protected void sendResultEvent(int result){
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
        int result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {

            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "onActivityResult: user accepted admin permissions request");
                result = performOperation(m_ev);
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(LOG_TAG, "onActivityResult: user declined admin permissions request");
                result = SmmCommonConstants.DESCMO_OPERATION_CANCELED;
            } else
                result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
            if(result != SmmCommonConstants.DESCMO_OPERATION_ASYNC)
                sendResultEvent(result);
        }
        if(result != SmmCommonConstants.DESCMO_OPERATION_ASYNC)
            finish();
    }

    @Override
    protected void newEvent(Event receivedEvent) {
        super.newEvent(receivedEvent);
        Log.d(LOG_TAG, "+newEvent");

        String eventName = receivedEvent.getName();
        if (eventName.equals("MSG_DESCMO_USER_INTERACTION_TIMEOUT")) {
            // finish activity causes onActivityResult, which finishes our activity
            finishActivity(REQUEST_CODE_ENABLE_ADMIN);
        }
        else {
            m_ev = new Event(receivedEvent);
            if (isAdmin(this, m_dpm)) {
                Log.d(LOG_TAG, "newEvent: application already has Admin permissions");
                int result = performOperation(m_ev);
                if(result != SmmCommonConstants.DESCMO_OPERATION_ASYNC)
                {
                    sendResultEvent(result);
                    finish();
                }
            }
            else
                startAdminPermissionActivity(this, m_adminName);
        }
    }
    
    public static void startAdminPermissionActivity(Activity ActivityCtx, ComponentName adminName) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                ActivityCtx.getString(R.string.swm_device_admin_explanation));
        ActivityCtx.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
    }

    public static boolean isAdmin(Context ctx, DevicePolicyManager dpm) {
        ComponentName cn = new ComponentName(ctx.getApplicationContext(), DmcDeviceAdminReceiver.class);

        if (dpm.isAdminActive(cn))
            return true;

        return false;
    }
}
