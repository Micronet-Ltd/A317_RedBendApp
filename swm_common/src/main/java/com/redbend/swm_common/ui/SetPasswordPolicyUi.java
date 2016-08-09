/*
 *******************************************************************************
 *
 * SetPasswordPolicyUi.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.ui;

import java.util.concurrent.TimeUnit;

import android.app.admin.DevicePolicyManager;
import android.util.Log;
import android.provider.Settings;
import com.redbend.app.Event;
import com.redbend.swm_common.DescmoTokenizer;
import com.redbend.swm_common.SmmCommonConstants;

/**
 * Set password policies.
 */
public class SetPasswordPolicyUi extends AdminUiBase {

	@Override
	protected int performOperation(Event event)
	{
		int result;
		
		try {
			Log.d(LOG_TAG, "+performOperation");
			byte[] props = m_ev.getVarStrValue("VAR_DESCMO_FEATURE_PROPS");
			if (props == null)
				return SmmCommonConstants.DESCMO_OPERATION_FAILED;
				
			String featureProps =  new String(props);
	
			DescmoTokenizer.printValue("feature-Props", featureProps);

			String enablePassPolicy = DescmoTokenizer.getStringProp(featureProps, SmmCommonConstants.DESCMO_STATUS_FIELD_NAME);
			DescmoTokenizer.printValue("password-policy-enable", enablePassPolicy);

			int passQuality = (enablePassPolicy != null && enablePassPolicy.equals("on")) ? 
				DevicePolicyManager.PASSWORD_QUALITY_COMPLEX :
				DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
			m_dpm.setPasswordQuality(m_adminName, passQuality);

			int minAlpha = 
					DescmoTokenizer.getBoolProp(featureProps, "AtLeastOneAlphChar")?1:0;
			DescmoTokenizer.printValue("minimum-aplph", minAlpha);
			m_dpm.setPasswordMinimumLetters(m_adminName, minAlpha);

			int minDigits = 
					DescmoTokenizer.getBoolProp(featureProps, "AtLeastOneNumber")?1:0;
			DescmoTokenizer.printValue("minimum-numeric", minDigits);
			m_dpm.setPasswordMinimumNumeric(m_adminName, minDigits);

			int minSpecial =
					DescmoTokenizer.getBoolProp(featureProps, "AtLeastOneSpecialChar")?1:0;
			DescmoTokenizer.printValue("minimum-symbols", minSpecial);
			m_dpm.setPasswordMinimumSymbols(m_adminName, minSpecial);

			int length = DescmoTokenizer.getIntProp(featureProps, "MinNumberOfChar");
			if (length == SmmCommonConstants.DESCMO_INT_PROP_DISABLE_VALUE)
			{
				DescmoTokenizer.printValue("minimum-length", length);
				m_dpm.setPasswordMinimumLength(m_adminName, 0);
			}
			else
			{
				int maxLength =
						m_dpm.getPasswordMaximumLength(DevicePolicyManager.PASSWORD_QUALITY_COMPLEX);
				if (length > maxLength)
					length = maxLength;
				else if (length > 0 && length < 3)
				{
					/* To avoid contradicting definitions of length and minAlpha,
					 * minDigits, minSpecial. */
					length = 3;
				}
				DescmoTokenizer.printValue("minimum-length", length);
				m_dpm.setPasswordMinimumLength(m_adminName, length);
			}

			int expirationTimeout = DescmoTokenizer.getIntProp(featureProps, "DayBetweenChange");
			if(expirationTimeout == SmmCommonConstants.DESCMO_INT_PROP_DISABLE_VALUE) 
					expirationTimeout = 0;
			DescmoTokenizer.printValue("expiration-timeout (in Days)", expirationTimeout);
			m_dpm.setPasswordExpirationTimeout(m_adminName,  TimeUnit.DAYS.toMillis(expirationTimeout));

			int historyRestriction = DescmoTokenizer.getIntProp(featureProps, "PasswordHist");
			if (historyRestriction == SmmCommonConstants.DESCMO_INT_PROP_DISABLE_VALUE) 
					historyRestriction = 0;
			DescmoTokenizer.printValue("history-length", historyRestriction);
			m_dpm.setPasswordHistoryLength(m_adminName, historyRestriction);

			int inactivityTimeLock = DescmoTokenizer.getIntProp(featureProps, "AutoLock");
			if (inactivityTimeLock != SmmCommonConstants.DESCMO_INT_PROP_DISABLE_VALUE)
			{
				DescmoTokenizer.printValue("max-time-to-lock (in Minutes)", inactivityTimeLock);
				long lValue = TimeUnit.MINUTES.toMillis(inactivityTimeLock);
				m_dpm.setMaximumTimeToLock(m_adminName, lValue);
				Settings.System.putLong(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
						lValue);
			} 
			else 
			{
				DescmoTokenizer.printValue("max-time-to-lock (in Minutes)", 0);
				m_dpm.setMaximumTimeToLock(m_adminName, 0);
				Settings.System.putLong(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
						2*60*1000);//put 2 minutes as it sounds good enough
			}

			int maxFailedAttempts = DescmoTokenizer.getIntProp(featureProps, "MaxFailedAttempt");
			if(maxFailedAttempts == SmmCommonConstants.DESCMO_INT_PROP_DISABLE_VALUE) 
				maxFailedAttempts = 0;
			DescmoTokenizer.printValue("max-failed-attempts", maxFailedAttempts);
			m_dpm.setMaximumFailedPasswordsForWipe(m_adminName, maxFailedAttempts);

			result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
		} catch (SecurityException e) {
			result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
		}
		
		return result;
	}
}
