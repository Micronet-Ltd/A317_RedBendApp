/*
 *******************************************************************************
 *
 * LockingHandler.java
 *
 * Fully lock device (LAWMO).
 *
 * Receives DIL events:
 * DMA_MSG_LAWMO_LOCK_LAUNCH
 *
 * Sends BL events:
 * DMA_MSG_LAWMO_LOCK_ENDED_FAILURE
 * DMA_MSG_LAWMO_LOCK_ENDED_SUCCESS
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import com.redbend.app.Event;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

/**
 * Lock device (LAWMO).
 */
public class LockingHandler extends LawmoHandlerBase{
	
	private String CLASS_NAME = this.getClass().getSimpleName();
	protected String m_password;
	protected boolean m_pwdFromServer;
	
	public LockingHandler(Context ctx) {
		super(ctx);
		m_password = null;
		m_pwdFromServer = false;
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
	
	private static String getRandomPassword() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}
	
	private synchronized boolean setPassword(String newPassword) {
		boolean result = false;
		
		try {
			// Set registry file to indicate that random password has been set
			FileOutputStream output;
			output = ctx.openFileOutput(passwordSet, Context.MODE_PRIVATE);
			output.close();
			
			// Set password
			if (_dpm.resetPassword(newPassword, 0)) {
				Log.i(CLASS_NAME,  CLASS_NAME + ".setPassword:" + "password was set");
				result = true;
			}
			else {
				Log.i(CLASS_NAME,  CLASS_NAME + ".setPassword:" + "password was not set");
			}
		} catch (IOException e) {
			Log.w(CLASS_NAME,  CLASS_NAME + ".setPassword:" + "failed to open file");
		} catch (SecurityException e) {
			// SecurityException can be thrown due to permission error in case of HoneyComb
			Log.w(CLASS_NAME,  CLASS_NAME + ".setPassword SecurityException:" + "password was not set", e);
		}
		
		if (!result) {
			if (ctx.deleteFile(passwordSet))
				Log.d(CLASS_NAME, "passwordSet was deleted");
			else
				Log.d(CLASS_NAME, "passwordSet was NOT deleted");
		}

		return result;
	}
	
	@Override
	protected void handleEvent(Event ev) {
		try{
			String encodedPassword = new String(ev.getVarStrValue("DMA_VAR_LAWMO_PASSWORD"));
			byte[] decodedPassword = Base64.decode(encodedPassword, Base64.DEFAULT);
			m_password = new String(decodedPassword, "UTF-8");
			m_pwdFromServer = true;
		}catch(Exception e){
			//DMA_VAR_LAWMO_PASSWORD=null so new String(null) 
			//will throw an exception 
			Log.d(CLASS_NAME, "password from server not used");
		}
	}
	
	@Override
	protected int tryOperation(Context context) throws SecurityException {
		int result = 1;
		File reg = ctx.getFileStreamPath(passwordSet);
		boolean fileExist = reg.exists();
		boolean policyConfigured = isPasswordPolicyConfigured() ;
		
		String newPassword;
		if (m_pwdFromServer == true)
			newPassword = m_password;
		else
			newPassword = getRandomPassword();
		
		boolean pswConfigured = setPassword(newPassword);
			
		if ( (m_pwdFromServer==false && fileExist) || (m_pwdFromServer==false && policyConfigured) || !pswConfigured) {
			Log.w(CLASS_NAME,  CLASS_NAME + ".tryOperation:" + "device was not locked fileExist:" + fileExist
					+ " policyConfigured:" + policyConfigured+ " pswConfigured:" + pswConfigured + " pwdFromServer:" + m_pwdFromServer);
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
