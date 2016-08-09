/*
 *******************************************************************************
 *
 * SetEncryptionPolicyUi.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common.ui;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.swm_common.DescmoTokenizer;
import com.redbend.swm_common.SmmCommonConstants;

/**
 * Set device encryption.
 */
public class SetEncryptionPolicyUi extends AdminUiBase {
	static final int START_ENCRYPTION_RESULT = 999;
	@Override
	protected int performOperation(Event event)
	{
		int result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;

		try {
			if ("MSG_DESCMO_SET_FEATURE".equals(event.getName()))
			{
				String featureProps =  new String(m_ev.getVarStrValue("VAR_DESCMO_FEATURE_PROPS"));
				DescmoTokenizer.printValue("feature-Props", featureProps);

				String enablePolicy = DescmoTokenizer.getStringProp(featureProps, SmmCommonConstants.DESCMO_STATUS_FIELD_NAME);
				DescmoTokenizer.printValue("policy-enable", enablePolicy);
				if (enablePolicy.equals("on"))
				{
					boolean applyEncryption = DescmoTokenizer.getBoolProp(featureProps, "ApplyEncryption");
					DescmoTokenizer.printValue("apply-encryption", applyEncryption);
					if (applyEncryption)
					{ 	
						int encStatus = m_dpm.getStorageEncryptionStatus();
						Log.d(LOG_TAG,"performOperation encryption status:" + encStatus);
						if ( encStatus == DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED)
							result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
						else if (encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE)
							result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
						else if (encStatus == DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE)
						{
							// Activate encryption
							Log.d(LOG_TAG,"performOperation applying encryption");
							int adminRes = m_dpm.setStorageEncryption(m_adminName, true);
							if (adminRes == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE)
							{
								Intent intent = new Intent(DevicePolicyManager.ACTION_START_ENCRYPTION);
								startActivityForResult(intent, START_ENCRYPTION_RESULT);
								result = SmmCommonConstants.DESCMO_OPERATION_ASYNC;
							}
							else 
								result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
						}
						else
							result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
					}
					else
					{
						m_dpm.setStorageEncryption(m_adminName, false);
						result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
					}
				}
			}
			else if("MSG_DESCMO_GET_FEATURE_STATUS".equals(event.getName()))
			{
				int encStatus = m_dpm.getStorageEncryptionStatus();

				Log.d(LOG_TAG, String.format("getStorageEncryptionStatus returned %x", encStatus));
				if ( encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE)
					result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
				else
					result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
			}
			else if("MSG_DESCMO_USER_INTERACTION_TIMEOUT".equals(event.getName()))
			{// if trying to activate encryption for too much time - stop the activation
				m_dpm.setStorageEncryption(m_adminName, false);
			}
		} catch (SecurityException e) {
			result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
			Log.d(LOG_TAG,e.toString());
		}
		
		return result;
	}
	 @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			int result = SmmCommonConstants.DESCMO_OPERATION_FAILED;
			
			if (requestCode == START_ENCRYPTION_RESULT)
			{
				int afterApllyingStatus = m_dpm.getStorageEncryptionStatus();
				Log.d(LOG_TAG,"performOperation after encryption status:" + afterApllyingStatus);
				if (afterApllyingStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE)
					result = SmmCommonConstants.DESCMO_OPERATION_SUCCESS;
			}
			sendResultEvent(result);
			finish();
	}
}
