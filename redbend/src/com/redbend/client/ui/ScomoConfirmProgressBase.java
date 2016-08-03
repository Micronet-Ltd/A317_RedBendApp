/*
 *******************************************************************************
 *
 * ScomoConfirmProgressBase.java
 *
 * Base class for:
 * - ScomoConfirm
 * - ScomoDownloadInterrupted
 * - ScomoDownloadProgress
 * - ScomoInstallConfirm
 * - ScomoInstallProgress
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.Vector;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import com.redbend.client.R;
import com.redbend.client.uialerts.DdTextHandler;


public abstract class ScomoConfirmProgressBase extends DilActivity {
	
	protected final static String DMA_MSG_SCOMO_ACCEPT = "DMA_MSG_SCOMO_ACCEPT";
	protected final static String DMA_MSG_SCOMO_POSTPONE = "DMA_MSG_SCOMO_POSTPONE";
	protected final static String DMA_MSG_SCOMO_CANCEL = "DMA_MSG_SCOMO_CANCEL";
	protected final static String DMA_VAR_IS_POSTPONE_ENABLED = "DMA_VAR_IS_POSTPONE_ENABLED";
	protected final static String DMA_VAR_SCOMO_CRITICAL = "DMA_VAR_SCOMO_CRITICAL";
	protected final static String DMA_VAR_DP_SIZE = "DMA_VAR_DP_SIZE";
	protected final static String DMA_VAR_DP_SIZE_MULTI = "DMA_VAR_DP_SIZE_MULTI";
	private static final char DELIMITER = 0x1F;
	
	protected void createReleaseNotes(Event ev, int inTextViewResId) {
	    // Add release notes link AKA info URL
	    final String infoUrl = DdTextHandler.getDdInfoUrl(ev);
	    if (infoUrl != null && URLUtil.isValidUrl(infoUrl)) {
	        // In the following implementation the URL is
	        // added to the UI after the DP components
	        // details
	        // as a link to the browser. The URL can also
	        // sent to other activity by onClick event or
	        // any
	        // other method.
	        
	        String infoUrlStr = "<a href=\"" + infoUrl + "\">"
	                + getString(R.string.release_notes) + "</a>";
	        TextView scomoInstInfoUrl = ((TextView) findViewById(inTextViewResId));
	        scomoInstInfoUrl.setText(Html.fromHtml(infoUrlStr));
	        scomoInstInfoUrl
	        .setMovementMethod(LinkMovementMethod.getInstance());
	    }
	}	

	protected String formatSize(double size){
		Log.d(LOG_TAG, "formatSize:" + size);
		long BASE = 1024, KB = BASE, MB = KB*BASE, GB = MB*BASE;		
		BigDecimal valDec = null;
		String valueType = null;
		
		if(size >= GB) {		
			valDec = new BigDecimal(String.valueOf(size/GB)); 
			valueType = getString(R.string.GB);
		}
		else if(size >= MB) {
			valDec = new BigDecimal(String.valueOf(size/MB)); 
			valueType = getString(R.string.MB);
		}
		else if(size >= KB) {
			valDec = new BigDecimal(String.valueOf(size/KB)); 
			valueType = getString(R.string.KB);
		}
		if (valDec != null && valueType != null) {
			BigDecimal roundedDec = valDec.setScale(1, BigDecimal.ROUND_UP);
			DecimalFormat df = new DecimalFormat("#.#");
			return df.format(roundedDec.doubleValue()) + " " + valueType;
		}
		
		return (int) size + " " + getString(R.string.bytes);
	}	
	
	private String getDpMultiSizes(Event ev) {
		String dpSizes = null;
		byte dpSizesByte [] =  ev.getVarStrValue(DMA_VAR_DP_SIZE_MULTI);
		if (dpSizesByte != null){
			dpSizes = new String(dpSizesByte);  
			dpSizes = dpSizes.replace(DELIMITER, ',');
			// if 1 DP file use the int size and format
			int numberOfDPSizes = dpSizes.split(",").length;
			if (numberOfDPSizes <= 1){
				int dpSize = ev.getVarValue(DMA_VAR_DP_SIZE);
				if (dpSize != 0)
					dpSizes = formatSize(dpSize);		
			}
		}
		return dpSizes;
	}
	
	protected String getDpSizeText(Event ev){
		String retVal = "";
		String dpSize = getDpMultiSizes(ev);
		
		if (dpSize != null)
			retVal = getString(R.string.size) + ": " + dpSize;		
		return retVal;
	}
	
	protected static String getAppListString(Context inCtx, Event ev, boolean inUpdate){		
		Vector<DdTextHandler.AppNameVersion> appNameVersions = DdTextHandler.getAppliacationsNames(ev, inUpdate);
		
		if(appNameVersions == null || appNameVersions.size() == 0){
			return "";
		}
		
		StringBuilder appsList = new StringBuilder();		
		for(DdTextHandler.AppNameVersion appNameVersion: appNameVersions){			
			StringBuilder applicationAttribute = new StringBuilder();
			if (!inUpdate) {
				// version is all zeroes, add name only to dpRemoveText
				applicationAttribute.append("-");
				applicationAttribute.append(appNameVersion.m_name);
				applicationAttribute.append("\n");
			} 
			else {
				// version is not all zeroes, add name + version to dpUpdateText				
				String  nameVersion ;
				if (appNameVersion.m_version.length() > 0 && appNameVersion.m_name.length() > 0) { 
					// If got version, then return full pkg name
					Formatter format = new Formatter();
					nameVersion = 
						format.format(inCtx.getString(R.string.sw_component_string), appNameVersion.m_name, appNameVersion.m_version).toString();
					format.close();
				}
				else {
					// return only name without version
					nameVersion = appNameVersion.m_name;
				}
				applicationAttribute.append("-");
				applicationAttribute.append(nameVersion);
				applicationAttribute.append("\n");
			}
			appsList.append(applicationAttribute);
		}
		return appsList.toString();
	}
}
