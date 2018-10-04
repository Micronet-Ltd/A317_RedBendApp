/*
 *******************************************************************************
 *
 * ConnectivityStateChangeReceiver.java
 *
 * Handles network (carrier and Wi-Fi) connectivity changes.
 *
 * Sends BL events:
 * DMA_MSG_STS_MOBILE_DATA
 * DMA_MSG_STS_WIFI
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmReceive;

/**
 * Handle network connectivity change.
 */
public class ConnectivityStateChangeReceiver extends SmmReceive {

	private abstract class DataChange {
		private boolean lastState;

		public DataChange(boolean initialState) {
			lastState = initialState;
		}

		abstract protected String getEventName();
		abstract protected String getVarName();
		public final synchronized void sendConnectedEvent(Context context, boolean isConnected)
		{
			if (isConnected == lastState)
				return;

			Event event = new Event(getEventName());
			event.addVar(new EventVar(getVarName(), isConnected ? 1:0));
			if (context instanceof ClientService) {
				Log.i(LOG_TAG, "sending connect update status directly using ClientService");
				((ClientService) context).sendEvent(event);
			}
			else
				SmmReceive.sendEvent(context, ClientService.class, event);

			lastState = isConnected;
		}
		public String getName() {
			return getClass().getSimpleName();
		}
	}

	private class MobileData extends DataChange {

		public MobileData(boolean initialState) {
			super(initialState);
		}
		@Override
		protected String getEventName() {
			return "DMA_MSG_STS_MOBILE_DATA";
		}
		@Override
		protected String getVarName() {
			return "DMA_VAR_STS_IS_MOBILE_DATA_CONNECTED";
		}
	}

	private class WifiState extends DataChange {

		public WifiState(boolean initialState) {
			super(initialState);
		}
		@Override
		protected String getEventName() {
			return "DMA_MSG_STS_WIFI";
		}
		@Override
		protected String getVarName() {
			return "DMA_VAR_STS_IS_WIFI_CONNECTED";
		}
	}

	private WifiState wifiState;
	private MobileData mobileData;

	public ConnectivityStateChangeReceiver(boolean initialWifiState, boolean initialDataState) {
		wifiState = new WifiState(initialWifiState);
		mobileData = new MobileData(initialDataState);
	}
	
	public final void sendUpdate(Context context, NetworkInfo networkInfo) {
		DataChange data;
		
		switch (networkInfo.getType()) {
		case ConnectivityManager.TYPE_MOBILE:
			data = mobileData;
			break;
		case ConnectivityManager.TYPE_WIFI:
		case ConnectivityManager.TYPE_ETHERNET:
			data = wifiState;
			break;
		default:
			return;
		}

		if (networkInfo.isConnected()){
			Log.d(LOG_TAG, data.getName() + " was connected");
			data.sendConnectedEvent(context, true);
		} else {
			Log.d(LOG_TAG, data.getName() + " was disconnected");
			data.sendConnectedEvent(context, false);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
			return;

		NetworkInfo networkInfo = (NetworkInfo)
				intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		
		sendUpdate(context, networkInfo);
	}
}
