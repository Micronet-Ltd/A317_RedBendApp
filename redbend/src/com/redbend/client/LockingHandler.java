/*
 *******************************************************************************
 *
 * LockingHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Log;

/**
 * Lock device (LAWMO).
 */
public class LockingHandler extends LawmoHandlerBase{
	
	private String CLASS_NAME = this.getClass().getSimpleName();
	
	public LockingHandler(Context ctx) {
		super(ctx);
	}
	
	// Check that no password policy was configured by other admin apps -
	// if not: the DMA app won't be able to set password to an empty one and unlock the device
	private boolean isPasswordPolicyConfigured() {
		boolean result = true;
	
    	if ((_dpm.getPasswordQuality(null) == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) &&
    			(_dpm.getPasswordMinimumLength(null) == 0)) {
    		Log.i(CLASS_NAME, CLASS_NAME + ".isPasswordPolicyConfigured:" +
    				"no password policy preconfigured");
    		result = false;
    	}
		else {
    		Log.i(CLASS_NAME, CLASS_NAME + ".isPasswordPolicyConfigured:" +
    				"password policy preconfigured");
		}

    	Log.d(CLASS_NAME, CLASS_NAME + ".isPasswordPolicyConfigured:" + "quality-" +
    			_dpm.getPasswordQuality(null) + " length-" + _dpm.getPasswordMinimumLength(null));

		return result;
    }	
	
	private synchronized boolean setRandomPassword() {
		boolean result = false;
		
		try {
			// Set registry file to indicate that random password has been set
			FileOutputStream output;
			output = ctx.openFileOutput(passwordSet, Context.MODE_PRIVATE);
			output.close();
			
			// Create password
			SecureRandom random = new SecureRandom();
			String newPassword = new BigInteger(130, random).toString(32);
			
			// Set password
			if (_dpm.resetPassword(newPassword, 0)) {
				Log.i(CLASS_NAME,  CLASS_NAME + ".setRandomPassword:" + "random password is set");
				result = true;
			}
			else {
				Log.i(CLASS_NAME,  CLASS_NAME + ".setRandomPassword:" + "random password is not set");
			}
		} catch (IOException e) {
			Log.w(CLASS_NAME,  CLASS_NAME + ".setRandomPassword:" + "failed to open file");
		} catch (SecurityException e) {
			// SecurityException can be thrown due to permission error in case of HoneyComb
			Log.w(CLASS_NAME,  CLASS_NAME + ".setRandomPassword:" + "random password is not set", e);
		}
		
		if (!result) {
			ctx.deleteFile(passwordSet);
		}

		return result;
	}
	
	@Override
	protected int tryOperation(Context context) throws SecurityException {
		int result = 1;
		File reg = ctx.getFileStreamPath(passwordSet);
		boolean fileExist = reg.exists();
		boolean policyConfigured = isPasswordPolicyConfigured() ;
		boolean pswConfigured = setRandomPassword();
		if ( fileExist || policyConfigured || !pswConfigured) {
			Log.w(CLASS_NAME,  CLASS_NAME + ".setRandomPassword:" + "random password is not set fileExist:" + fileExist
					+ " policyConfigured:" + policyConfigured+ " pswConfigured:" + pswConfigured );
			result = 0;
		}
		else {
			_dpm.lockNow();
		}
		
		return result ;
	}
	@Override
	protected String getResultStringForDebug(int result) {
		if (result == 0)
			return "Screen wasn't locked";
		return "Screen is locked";
	}
	@Override
	protected String getEvent(int result) {
		if (result == 0)
			return "DMA_MSG_LAWMO_LOCK_ENDED_FAILURE";
		return "DMA_MSG_LAWMO_LOCK_ENDED_SUCCESS";
	}
}
