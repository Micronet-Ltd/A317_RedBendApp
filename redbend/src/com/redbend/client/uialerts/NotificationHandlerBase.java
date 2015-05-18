/*
 *******************************************************************************
 *
 * NotificationHandlerBase.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import java.util.Formatter;
import java.util.Vector;

import android.app.Notification;
import android.content.Context;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.client.R;
import com.redbend.swm_common.SmmCommonConstants;

public abstract class NotificationHandlerBase extends EventHandler {
	
	protected final String LOG_TAG = getClass().getSimpleName();
	
	protected int m_resIdSingleApp = -1; //resource for string if there is one application only
	protected int m_resIdAppsList = -1; //resource for string if there are a few applications
	protected int m_resIdContentText = -1; //resource for a string in content text line, e.g. "Downloading"  
	protected int m_appListLen = 0;
	protected String m_appsList = null;
	protected Notification.Builder m_notificationBuilder = null;
	protected String m_eventName = null;
	protected int m_largeIconResId;
	protected int m_smallIconResId;
	protected boolean  m_isInstall = true;
	protected static int ERROR = -1;
	protected static int SUCCESS = 0;
	protected MoType m_moType;

	public NotificationHandlerBase(Context ctx) {
		super(ctx);
		// TODO Auto-generated constructor stub
	}
	
	protected enum MoType{
		SCOMO,
		FUMO,
		FUMO_IN_SCOMO
	}
	
	private static String formatApplicationNamesToString(
			Vector<DdTextHandler.AppNameVersion> inFullApllicationNames) {
		StringBuilder appList = new StringBuilder();
		int counter = 0;
		for (DdTextHandler.AppNameVersion fullApllicationName : inFullApllicationNames) {
			if (counter > 0) {
				appList.append(", ");
			}
			appList.append(fullApllicationName.m_name);
			counter++;
		}
		return appList.toString();
	}
	
	protected void prepareApplicationNamesString(Event ev){
		m_appListLen = 0;
		m_appsList = null;
		m_isInstall = true;
		if(m_moType == MoType.FUMO){
			m_appListLen = 1;
			m_appsList = ctx.getString(R.string.fumo_update);
		} else {
			Vector<DdTextHandler.AppNameVersion> appNameVersion = DdTextHandler
					.getAppliacationsNames(ev, true);
			if (appNameVersion == null) {
				return;
			}
			if (appNameVersion.size() == 0) {
				m_isInstall = false;
				appNameVersion = DdTextHandler.getAppliacationsNames(ev, false);
			}
			m_appListLen = appNameVersion.size();
			m_appsList = formatApplicationNamesToString(appNameVersion);
		}
	}
	
	// create string for ticker text:
	protected String getTickerText() {		
		if (m_appListLen == 0) {
			return "";
		}		
		
		StringBuilder textResult = new StringBuilder();
		Formatter formatter = new Formatter();
		if(m_appListLen == 1){
			textResult.append(formatter.format(
					ctx.getString(m_resIdSingleApp), m_appsList)
					.toString());
		}else {
			textResult.append(formatter.format(
					ctx.getString(m_resIdAppsList), m_appListLen).toString());
		}
		
		formatter.close();
		return textResult.toString();
	}
	
	// Get apps list names if there are more than one app else return 
	// a predefined string, e.g.: installing 
	protected String getContentText() {
		if (m_appListLen == 0) {
			return "";
		}
		
		StringBuilder textResult = new StringBuilder();
		if(m_appListLen == 1){
			textResult.append(ctx.getString(m_resIdContentText));
		}else { 
			textResult.append(m_appsList);
		}
		
		return textResult.toString();
	}
	
	// 
	protected String getContentTitle() {		
		if (m_appListLen == 0) {
			return "";
		}
		StringBuilder textResult = new StringBuilder();
		if(m_appListLen == 1){
			textResult.append(m_appsList);
		}else {
			Formatter formatter = new Formatter();
			textResult.append(formatter.format(
					ctx.getString(m_resIdAppsList), m_appListLen).toString());
			formatter.close();
		}
		return textResult.toString();
	}
	
	protected static MoType getMoType(int inMoType) {
		switch (inMoType) {
		case SmmCommonConstants.E_DP_Type_Fumo:
			return MoType.FUMO;
		case SmmCommonConstants.E_DP_Type_FumoInScomo:
			return MoType.FUMO_IN_SCOMO;
		default:
			return MoType.SCOMO;
		}
	}
	
	protected abstract int initiate(Event ev);
}
