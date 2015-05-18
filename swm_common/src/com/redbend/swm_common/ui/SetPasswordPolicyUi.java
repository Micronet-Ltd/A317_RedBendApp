/*
 *******************************************************************************
 *
 * SetPasswordPolicyUi.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.ui;

import java.util.concurrent.TimeUnit;
import android.provider.Settings;
import android.util.Log;
import android.app.admin.DevicePolicyManager;
import com.redbend.swm_common.SmmCommonConstants;

/**
 * Set password policies.
 */
public class SetPasswordPolicyUi extends AdminUiBase {
	private void printValue(String policy, int value)
	{
		Log.d(LOG_TAG, "Setting password policy " + policy + "=" + value);
	}

	@Override
	protected int performOperation()
	{
		int result;
		
		try {
			int enablePassPolicy = m_ev.getVarValue("VAR_DESCMO_PASS_POLICY_ENABLE");
			printValue("password-policy-enable", enablePassPolicy);
			int passQuality = enablePassPolicy == 0 ? 
				DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED : 
				DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
			m_dpm.setPasswordQuality(m_adminName, passQuality);

			int minAlpha = m_ev.getVarValue("VAR_DESCMO_MIN_NUM_OF_ALPHA_CHARS");
			printValue("minimum-aplph", minAlpha);
			m_dpm.setPasswordMinimumLetters(m_adminName, minAlpha);

			int minDigits = m_ev.getVarValue("VAR_DESCMO_MIN_NUM_OF_DIGIT_CHARS");
			printValue("minimum-numeric", minDigits);
			m_dpm.setPasswordMinimumNumeric(m_adminName, minDigits);

			int minSpecial = m_ev.getVarValue("VAR_DESCMO_MIN_NUM_OF_SPECIAL_CHARS");
			printValue("minimum-symbols", minSpecial);
			m_dpm.setPasswordMinimumSymbols(m_adminName, minSpecial);

			int length = m_ev.getVarValue("VAR_DESCMO_MIN_NUM_OF_CHARS");
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
			printValue("minimum-length", length);
			m_dpm.setPasswordMinimumLength(m_adminName, length);

			int expirationTimeout = m_ev.getVarValue("VAR_DESCMO_EXPIRATION_TIMEOUT_DAYS");
			printValue("expiration-timeout (in Days)", expirationTimeout);
			m_dpm.setPasswordExpirationTimeout(m_adminName, TimeUnit.DAYS.toMillis(expirationTimeout));

			int historyRestriction = m_ev.getVarValue("VAR_DESCMO_HISTORY_RESTRICTION");
			printValue("history-length", historyRestriction);
			m_dpm.setPasswordHistoryLength(m_adminName, historyRestriction);

			int inactivityTimeLock = m_ev.getVarValue("VAR_DESCMO_INACTIVITY_TIME_LOCK_MIN");
			printValue("max-time-to-lock (in Minutes)", inactivityTimeLock);
			long lValue = TimeUnit.MINUTES.toMillis(inactivityTimeLock);
			m_dpm.setMaximumTimeToLock(m_adminName, lValue);
			Settings.System.putLong(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
					lValue);

			int maxFailedAttempts = m_ev.getVarValue("VAR_DESCMO_MAX_FAILED_ATTEMPTS");
			printValue("max-failed-attempts", maxFailedAttempts);
			m_dpm.setMaximumFailedPasswordsForWipe(m_adminName, maxFailedAttempts);

			result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
		} catch (SecurityException e) {
			result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
		}
		
		return result;
	}
}
