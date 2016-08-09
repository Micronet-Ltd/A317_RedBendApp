/*
 *******************************************************************************
 *
 * LawmoHandlerBase.java
 *
 * Base abstract class for handling LAWMO events Implements common logic that
 * includes checking for permission and sending a result event. Each LAWMO
 * operation implements this class.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
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
	protected abstract void handleEvent(Event ev);
	
	protected LawmoHandlerBase(Context ctx) {
		super(ctx);
	}
	
	@Override
	protected void genericHandler(Event ev) {
		Log.v(CLASS_NAME, CLASS_NAME + " started");
		Event event;
		int result = 1;
		
		handleEvent(ev);
		
		_dpm = (DevicePolicyManager)ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
		
		if (AdminUiBase.isAdmin(ctx, _dpm)) {
			Log.v(CLASS_NAME, CLASS_NAME + ":device admin is active");
			result = performOperation(ctx);
		}
		else {
			Log.v(CLASS_NAME, CLASS_NAME + ":device admin isn't active");
			result = 0;
		}
		
		event = new Event(getEvent(result));
		// if operation is success, we add reboot var to the event.
		if ("DMA_MSG_LAWMO_LOCK_ENDED_SUCCESS".equals(event.getName()) || 
				"DMA_MSG_LAWMO_UNLOCK_ENDED_SUCCESS".equals(event.getName())) {
			 int isRebootNeeded;
			// check if Lollipop and above to reboot the device
			if (result != 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				isRebootNeeded = 1;
			}
			else {
				isRebootNeeded = 0;
			}
			Log.d(CLASS_NAME, CLASS_NAME + event.getName() + ",DMA_VAR_LAWMO_REBOOT_NEEDED is: " + isRebootNeeded);
			event.addVar(new EventVar("DMA_VAR_LAWMO_REBOOT_NEEDED",	isRebootNeeded));
		}

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
