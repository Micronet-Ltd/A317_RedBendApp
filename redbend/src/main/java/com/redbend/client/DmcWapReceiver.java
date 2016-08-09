/*
 *******************************************************************************
 *
 * DmcWapReceiver.java
 *
 * Handles a WAP Push.
 *
 * Sends BL events:
 * DMA_MSG_NET_NIA: when a WAP Push is received. The WAP Push data is added to
 *                  DMA_VAR_NIA_MSG and DMA_VAR_NIA_ENCODED is set to 1 to
 *                  indicate binary data.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventVar;
import com.redbend.app.SmmReceive;

/**
 * Notify the BLL about a WAP Push.
 */
public class DmcWapReceiver extends SmmReceive
{
	private static final String NIA_CONTENT_TYPE = "C4";
			
	private static String byteArrayToHex(byte[] a) {
	   StringBuilder sb = new StringBuilder();
	   for(byte b: a)
	     sb.append(String.format("%02x", b&0xff));
	   return sb.toString();
	}
	
	private static String getContentTypeFromHeader(byte[] header) {
		char[] niaHeader = byteArrayToHex(header).toCharArray();
		StringBuilder sb = new StringBuilder();
	    sb.append(niaHeader[0]);
	    sb.append(niaHeader[1]);
		return sb.toString();
	}
	
	/*
     *   transactionId (Integer) - The WAP transaction ID
     *   pduType (Integer) - The WAP PDU type
     *   header (byte[]) - The header of the message
     *   data (byte[]) - The data payload of the message
     *   contentTypeParameters (HashMap<String,String>)
     *   - Any parameters associated with the content type 
     *   (decoded from the WSP Content-Type header)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		byte[] nia;
		byte[] header;		
		HashMap<String, String> contentType;
		
		Bundle bundle = intent.getExtras();
		/* The data payload of the message */
		nia = (byte[]) bundle.get("data");
		header = (byte[]) bundle.get("header");
		contentType = getContentType(bundle);
		for (String key : contentType.keySet())
			Log.d(LOG_TAG, "ContentTypeParam '" + key + "' value=" + contentType.get(key));
		
	    String nia_str = new String(nia);	    
	    String headerContentType  = getContentTypeFromHeader(header);
	   
	    if (!headerContentType.equalsIgnoreCase(NIA_CONTENT_TYPE)){
	    	Log.i(LOG_TAG, "received WAP_PUSH that isn't a NIA");
	    	return;
	    }
	    	
	    Log.d(LOG_TAG, "received WAP_PUSH containing \"" + nia_str + "\"");
		sendEvent(context, ClientService.class, new Event("DMA_MSG_NET_NIA")
			.addVar(new EventVar("DMA_VAR_NIA_MSG", nia))
			.addVar(new EventVar("DMA_VAR_NIA_ENCODED", 1))
			);
	}

	@SuppressWarnings("unchecked")
	private static HashMap<String, String> getContentType(Bundle bundle) {
		return (HashMap<String, String>) bundle.get("contentTypeParameters");
	}
}
