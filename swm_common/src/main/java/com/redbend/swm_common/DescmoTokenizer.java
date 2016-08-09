/*
 *******************************************************************************
 *
 * DescmoTokenizer.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import android.util.Log;

public class DescmoTokenizer {

	private static String LOG_TAG = "DescmoTokenizer";
	
	static public String getStringProp(String propsStr, String fieldName)
	{
		String propsArr[]  = propsStr.replaceAll("\\\\;", "<semicolon>").split(";");
		for (String prop : propsArr)
		{
			int splitPlaceHolder = prop.indexOf("=");
			String propName = prop.substring(0,splitPlaceHolder);
			propName.replaceAll("<semicolon>", ";");
			String propValue= prop.substring(splitPlaceHolder+1);
			propValue.replaceAll("<semicolon>", ";");
			Log.d(LOG_TAG, "getStringProp propName:" + propName + " propValue:" + propValue);
			if (propName.equals(fieldName))
				return propValue;
		}
		return null;
	}

	static public int getIntProp(String propsStr, String fieldName)
	{
		try{
			return Integer.valueOf(getStringProp(propsStr, fieldName));
		} catch (NumberFormatException e) {
			Log.e(LOG_TAG, e.toString());
			return 0;
		}
	}
	
	static public boolean getBoolProp(String propsStr, String fieldName)
	{
		return "true".equals(getStringProp(propsStr, fieldName)) ;
	}
	
	static public void printValue(String policy, int value)
	{
		Log.d(LOG_TAG, "Setting policy " + policy + "=" + value);
	}
	
	static public void printValue(String policy, boolean value)
	{
		Log.d(LOG_TAG, "Setting policy " + policy + "=" + value);
	}
	
	static public void printValue(String policy, String value)
	{
		Log.d(LOG_TAG, "Setting policy " + policy + "=" + value);
	}
}
