/*
 *******************************************************************************
 *
 * LawmoHandlerBase.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.swm_common.ui.AdminUiBase;

/**
 * Base abstract class for handling LAWMO events. Implements common logic that
 * includes checking for permission and sending a result event.
 *
 * Each type of LAWMO operation implements this class.
 */
public abstract class LawmoHandlerBase extends EventHandler {

	protected static String passwordSet = "passwordSet";
	protected static String PREFS_NAME = "appPrefs";
	private String CLASS_NAME = this.getClass().getSimpleName();
	protected DevicePolicyManager _dpm;
	
	// Abstract methods which must be implmeneted by the lock/unlock handlers
	protected abstract int tryOperation(Context context) throws SecurityException ;
	protected abstract String getResultStringForDebug(int result) ;
	protected abstract String getEvent(int result);
	
	protected LawmoHandlerBase(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected void genericHandler(Event ev) {
		Log.v(CLASS_NAME, CLASS_NAME + " started");
		Event event;
		int result = 1;
		
		_dpm = (DevicePolicyManager)ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
		
		if (AdminUiBase.enableAdmin(ctx, _dpm, false)) {
			Log.v(CLASS_NAME, CLASS_NAME + ":device admin is active");
			result = performOperation(ctx);
		}
		else {
			Log.v(CLASS_NAME, CLASS_NAME + ":device admin isn't active");
			result = 0;
		}
		
		event = new Event(getEvent(result));
		
		Log.e(CLASS_NAME, CLASS_NAME + " sending event " + event.getName());
		((ClientService)ctx).sendEvent(event);
	}
	
	protected int performOperation(Context context) {
		int result = 0;
		try {
			result = tryOperation(context);
			Log.e(CLASS_NAME,  CLASS_NAME + ".performOperation:" + getResultStringForDebug(result));
		} catch (SecurityException e) {
			// SecurityException can be thrown due to permission error in case of HoneyComb
			Log.w(CLASS_NAME, CLASS_NAME + ".performOperation: failed! - SecurityException" , e);
		}
		
		return result;
	}
}
