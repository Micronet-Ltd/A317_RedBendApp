/*
 *******************************************************************************
 *
 * DdTextHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import java.util.Vector;
import android.util.Log;
import com.redbend.app.Event;

/**
 * Parse the description field of a DD. The default description is defined in
 * DMA_VAR_FUMO_DP_DESCRIPTION.
 */
public class DdTextHandler {
	
	//Description parameter coming from engine has 3 fields:
	enum eDescriptionToken {
		DESCRIPTION_NAME,
		DESCRIPTION_VERSION,
		DESCRIPTION_FILE_NAME
	}
	
	private static final String VAR_FUMO_DP_DESCRIPTION = "DMA_VAR_FUMO_DP_DESCRIPTION";
	
	private static final String DEFAULT_LOCALE = "en_US";
	private static final String LOG_TAG = "DdTextHandler";
	private static final String TYPE_HTML_AND_LOCALE = "&Type=HTML&Locale=";
	private static final String UNDERSCORE = "_";
	private static final String DMA_VAR_DP_INFO_URL = "DMA_VAR_DP_INFO_URL";

	private static String getEventVarStr(Event ev, String id) {
		byte[] bytes = ev.getVarStrValue(id);
		return bytes != null ? new String(bytes) : null;
	}
	
	public static class AppNameVersion{		
		public AppNameVersion(String inName, String inVersion){
			m_name = inName;
			m_version = inVersion;
		}
		public String m_name;
		public String m_version;
	}
	
	// Get list of application names - (removed or updated, without versions)
	public static Vector<AppNameVersion> getAppliacationsNames(Event inEvent, boolean inUpdate) {
		String dpDesc = getEventVarStr(inEvent, VAR_FUMO_DP_DESCRIPTION);
		if (dpDesc == null) {
			return null;
		}

		Log.d(LOG_TAG, "getAppliacationsNames=>dpDesc: " + dpDesc);

		Vector<AppNameVersion> appList = new Vector<AppNameVersion>();
		AppNameVersion apllicationName;
		for (String componentToken : dpDesc.split(";")) {
			String name = getDdDescriptionToken(
					eDescriptionToken.DESCRIPTION_NAME, componentToken);
			String version = getDdDescriptionToken(
					eDescriptionToken.DESCRIPTION_VERSION, componentToken);
			
			int versionSum = 0;
			for (String versionPart : version.split("\\.")) {
				try {
					versionSum += Integer.valueOf(versionPart);
				} catch (NumberFormatException nfe) {
					versionSum += 9;
				}
			}
			if ((versionSum == 0 && !inUpdate) || (versionSum != 0 && inUpdate)) {
				apllicationName = new AppNameVersion(name, version);
				appList.add(apllicationName);
			}
		}
		return appList;
	}
	
	public static String getDdInfoUrl(Event ev) {
		StringBuilder urlBuffer = new StringBuilder();
		String infoUrlValue = getEventVarStr(ev, DMA_VAR_DP_INFO_URL);
		if (infoUrlValue == null) {
			return null;
		}
		urlBuffer = urlBuffer.append(infoUrlValue).append(TYPE_HTML_AND_LOCALE)
				.append(getLocale());
		Log.d(LOG_TAG, "-getDdInfoUrl: " + urlBuffer);
		return urlBuffer.toString();
	}

	private static String getLocale() {
		StringBuilder locale = new StringBuilder();
		String languageCode = java.util.Locale.getDefault().getLanguage();
		String countryCode = java.util.Locale.getDefault().getCountry();
		if (languageCode != null && languageCode.length() > 0
				&& countryCode != null && countryCode.length() > 0) {
			locale = locale.append(languageCode).append(UNDERSCORE)
					.append(countryCode);
		} else {
			locale = locale.append(DEFAULT_LOCALE);
		}
		Log.d(LOG_TAG, "-getLocale: " + locale);
		return locale.toString();
	}
	
	private static String getDdDescriptionToken(eDescriptionToken descToken, String dpDesc) {
		String tokens[] = dpDesc.split(",");
		if (tokens.length > descToken.ordinal())
			return tokens[descToken.ordinal()];
		return "";
	}
}
