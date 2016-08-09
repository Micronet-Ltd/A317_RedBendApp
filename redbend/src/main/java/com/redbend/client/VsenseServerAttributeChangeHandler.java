/*
 *******************************************************************************
 *
 * VsenseServerAttributeChangeHandler.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;
import com.redbend.app.EventVar;

/** 
 * Start a download if not in user mode.
 */
public class VsenseServerAttributeChangeHandler extends EventHandler {

	private final String LOG_TAG = getClass().getSimpleName();
	private static final String DMA_MSG_DM_DOMAIN_NAME = "DMA_MSG_DM_DOMAIN_NAME";
	private static final String DMA_MSG_DM_VSENSE_SERVER_ADDR = "DMA_MSG_DM_VSENSE_SERVER_ADDR";
	private static final String DMA_MSG_DM_VSENSE_POLLING_INTERVAL = "DMA_MSG_DM_VSENSE_POLLING_INTERVAL";
	private static final String DMA_VAR_DOMAIN_NAME = "DMA_VAR_DOMAIN_NAME";
	private static final String DMA_VAR_VSM_SERVER_ADDRR = "DMA_VAR_VSM_SERVER_ADDRR";
	private static final String DMA_VAR_VSM_POLLING_INTERVAL = "DMA_VAR_VSM_POLLING_INTERVAL";
	private static final String DMA_MSG_DM_VSENSE_SERVER_ATTR = "DMA_MSG_DM_VSENSE_SERVER_ATTR";	
	
	public static final String DOMAIN_NAME_CHANGED_INTENT_FILTER = "com.redbend.client.DOMAIN_NAME_CHANGED";
	public static final String SERVER_ADDRR_CHANGED_INTENT_FILTER = "com.redbend.client.SERVER_ADDRR_CHANGED";
	public static final String POLLING_INTERVAL_CHANGED_INTENT_FILTER = "com.redbend.client.POLLING_INTERVAL_CHANGED";
	public static final String SERVER_ATTR_CHANGED_INTENT_FILTER = "com.redbend.client.VSENSE_SERVER_ATTR_CHANGED";
	
	public static final String DOMAIN_NAME_CHANGED_EXTRA_DATA = "domain_name";
	public static final String SERVER_ADDRR_CHANGED_EXTRA_DATA = "server_addrr";
	public static final String POLLING_INTERVAL_EXTRA_DATA = "polling_interval";
	public static final String DEVICE_ID_EXTRA_DATA = "device_id";


	public VsenseServerAttributeChangeHandler(Context ctx) {
		super(ctx);
	}
	
	private void sendIntent(Event ev, String varName, String filterName, String extraName){
		Log.d(LOG_TAG, "+sendIntent::sendBroadcast intent: " + filterName + ", varName: " + varName);
		EventVar var = null;
		try {
			var = ev.getVar(varName);
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(LOG_TAG, "-sendIntent:: getVar exception");
			return;
		}
		
		if (var == null) {
			return;
		}
		Intent intent = new Intent(filterName);
		if(!varName.equals(DMA_VAR_VSM_POLLING_INTERVAL)){
			byte[] eventValue = null;
			if((eventValue = var.getStrValue()) == null){
				return;
			}
			String value = new String(eventValue);
			intent.putExtra(extraName, value);		
			Log.d(LOG_TAG, "genericHandler::eventValue = " + value);
		}else{
			int value = var.getValue();
			intent.putExtra(extraName, value);	
			Log.d(LOG_TAG, "genericHandler::eventValue polling interval = " + var.getValue());
		}		
		ctx.sendBroadcast(intent);
		Log.d(LOG_TAG, "-sendIntent");
	}
	
	private void sendIntent(Intent intent){
		Log.d(LOG_TAG, "+sendIntent::sendBroadcast prepared intent: " + intent.toString());
		ctx.sendBroadcast(intent);
		Log.d(LOG_TAG, "-sendIntent");
	}
	
	@Override
	protected void genericHandler(Event ev) {
		Log.d(LOG_TAG, "+genericHandler");
		if (ev == null) {
			Log.d(LOG_TAG, "-genericHandler:: ev == null");
			return;
		}
		String eventName = ev.getName();
		if (eventName == null) {
			Log.d(LOG_TAG, "-genericHandler:: eventName == null");
			return;
		}
		Log.d(LOG_TAG, "genericHandler::eventName = " + eventName);

		// Updating server attributes for Analytics - address, domain and interval
		if (eventName.equals(DMA_MSG_DM_VSENSE_SERVER_ATTR)){
			Intent i = new Intent(SERVER_ATTR_CHANGED_INTENT_FILTER);
			try {
				EventVar domainVar = ev.getVar(DMA_VAR_DOMAIN_NAME);
				String domainValue = new String(domainVar.getStrValue());
				i.putExtra(DOMAIN_NAME_CHANGED_EXTRA_DATA, domainValue);

				EventVar serverAddressVar = ev.getVar(DMA_VAR_VSM_SERVER_ADDRR);
				String serverValue = new String(serverAddressVar.getStrValue());
				i.putExtra(SERVER_ADDRR_CHANGED_EXTRA_DATA, serverValue);

				EventVar pollingIntervalVar = ev.getVar(DMA_VAR_VSM_POLLING_INTERVAL);
				int pollingValue = pollingIntervalVar.getValue();
				i.putExtra(POLLING_INTERVAL_EXTRA_DATA, pollingValue);
				
				String deviceId = Ipl.getDeviceId(ctx);
				i.putExtra(DEVICE_ID_EXTRA_DATA, deviceId);

				Log.d(LOG_TAG, "genericHandler:: Updating server address = " + serverValue +
						" domain = " + domainValue + " polling interval = " + pollingValue);
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(LOG_TAG, "-sendIntent:: getVar exception");
				return;
			}
			sendIntent(i);
		}

		if (eventName.equals(DMA_MSG_DM_VSENSE_SERVER_ATTR) || 
				eventName.equals(DMA_MSG_DM_DOMAIN_NAME)) {
			sendIntent(ev, DMA_VAR_DOMAIN_NAME,
					DOMAIN_NAME_CHANGED_INTENT_FILTER,
					DOMAIN_NAME_CHANGED_EXTRA_DATA);
		}
		if (eventName.equals(DMA_MSG_DM_VSENSE_SERVER_ATTR) ||
				eventName.equals(DMA_MSG_DM_VSENSE_SERVER_ADDR)) {
			sendIntent(ev, DMA_VAR_VSM_SERVER_ADDRR,
					SERVER_ADDRR_CHANGED_INTENT_FILTER,
					SERVER_ADDRR_CHANGED_EXTRA_DATA);
		}
		
		if (eventName.equals(DMA_MSG_DM_VSENSE_SERVER_ATTR) ||
				eventName.equals(DMA_MSG_DM_VSENSE_POLLING_INTERVAL)) {
			sendIntent(ev, DMA_VAR_VSM_POLLING_INTERVAL,
					POLLING_INTERVAL_CHANGED_INTENT_FILTER,
					POLLING_INTERVAL_EXTRA_DATA);
		}
		Log.d(LOG_TAG, "-genericHandler");
	}
}
