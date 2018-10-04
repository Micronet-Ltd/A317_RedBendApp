/*
 *******************************************************************************
 *
 * Event.java
 *
 * The event class; used for both BL and DIL events.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.*;
import java.util.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;


/** Holds message event */
@SuppressLint("UseSparseArrays")
public class Event {
	private String eventName;
	private HashMap<String, EventVar> vars;
	
	public final static String intentActionPrefix = "com.redbend.event.";
	
	private static final String MSG_SED_INTENT = "DMA_MSG_MNG_VM_SED_INTENT";
	private static final String MSG_PPD_INTENT = "DMA_MSG_MNG_VM_PPD_INTENT";
	private static final String MSG_SED_INTENT_FORWARD = "DMA_MSG_MNG_VM_SED_INTENT_FORWARD";
	private static final String MSG_PPD_INTENT_FORWARD = "DMA_MSG_MNG_VM_PPD_INTENT_FORWARD";
	public final static String DESTINATION_PKG_VAR_NAME = "DMA_VAR_INTENT_PKG_NAME";
	public final static String DMA_VAR_INTENT_DATA = "DMA_VAR_INTENT_DATA";
	private static final String ANDROID_PREFIX = "android.";
	
	public static byte[] readString(DataInputStream in) throws IOException {
		byte data[] = null;
		int str_len = in.readInt();
		
		if (str_len > 0)
		{
			data = new byte[str_len];
			in.read(data, 0, str_len);
		}
		return data;
	}
	
	public Event(String msgName) 
	{
		this.eventName = msgName;
		vars = new HashMap<String, EventVar>();
	}
	
	public Event(Event ev)
	{
		eventName = ev.eventName;
		vars = ev.vars;
	}
	
	public Event(byte input[]) throws IOException
	{
		/* TODO this could be optimized, because, normally the Event
		 * is generated for the sole purpose of sending it, so the byte array
		 * is parsed to the class' data, and later given as input to a stream */
		this(new ByteArrayInputStream(input));
	}
	
	private Event(InputStream in) throws IOException
	{
		this(new DataInputStream(in));
	}
	
	public Event(DataInputStream in) throws IOException
	{
		int vars_count;
		
		eventName = new String(readString(in));
		vars_count = in.readInt();
		vars = new HashMap<String, EventVar>(vars_count);

		while (vars_count-- > 0){
			EventVar var = new EventVar(in); 
			vars.put(var.getName(), var);
		}
	}
	
	public Event(Intent i) throws IOException
	{
		String action = i.getAction();
		Bundle varsExtra = i.getExtras();
		
		if (action == null || !action.startsWith(intentActionPrefix))
			throw new IOException("Invalid intent for event, action should start with " + intentActionPrefix);
		
		eventName = action.substring(intentActionPrefix.length());
		
		if (varsExtra == null) {
			vars = new HashMap<String, EventVar>(0);
			return;
		}
		
		vars = new HashMap<String, EventVar>(varsExtra.size());
		
		if (MSG_SED_INTENT.equals(eventName) || MSG_SED_INTENT_FORWARD.equals(eventName)
		    || MSG_PPD_INTENT.equals(eventName) || MSG_PPD_INTENT_FORWARD.equals(eventName)) {
			
			/* The intent forwarding events can contain only two variables.
			 * Marshall the entire extra data to "DMA_VAR_INTENT_DATA" for forwarding intent,
			 * so that all the intent extra data can be restored.
			 */
			vars.put(DMA_VAR_INTENT_DATA,
			    new EventVar(DMA_VAR_INTENT_DATA, ParcelableUtil.marshall(varsExtra)));
			String packageName = varsExtra.getString(DESTINATION_PKG_VAR_NAME);
			if (packageName != null) {
				vars.put(DESTINATION_PKG_VAR_NAME,new EventVar(DESTINATION_PKG_VAR_NAME, packageName));
			}
		} else {
			// create vars for SMM supported types for other SMM events 
			for (String varName : varsExtra.keySet()) {
				/* skipping extras that come from Android, this includes
				 * wakeful broadcasts additional extras */
				if (varName.startsWith(ANDROID_PREFIX)) {
					Log.i("SMM.Event", "Skipping intent extra " + varName);
					continue;
				}
				vars.put(varName, new EventVar(varName, varsExtra));
			}
		}
	}

	public Intent createIntent()
	{
		Intent i = new Intent();
		
		i.setAction(intentActionPrefix + eventName);

		if (MSG_SED_INTENT.equals(eventName) || MSG_SED_INTENT_FORWARD.equals(eventName)
	        || MSG_PPD_INTENT.equals(eventName) || MSG_PPD_INTENT_FORWARD.equals(eventName)) {
			
			// set target packge name if possible
			byte[] pkgNameBytes = getVarStrValue(DESTINATION_PKG_VAR_NAME);
			if(pkgNameBytes != null) {
				String pkgName = new String(pkgNameBytes);
				Log.i("SMM.Event", "Setting Event " + eventName + " destination pkg to " + pkgName);
				i.setPackage(pkgName);
			}
			
			byte[] extraBytes = getVarBinValue(DMA_VAR_INTENT_DATA);
			if (extraBytes != null) { // forwarded intent, DMA_VAR_INTENT_DATA variable already has everything
				Parcel extraParcel = ParcelableUtil.unmarshall(extraBytes);
				Bundle extras = Bundle.CREATOR.createFromParcel(extraParcel);
				i.putExtras(extras);
			}
		} else { // intent from SMM, iterate all the variables and put them into intent
		
    		for (EventVar v : vars.values()) {
    			/* optionally the event could contain data for destination pkg */
    			if (DESTINATION_PKG_VAR_NAME.equals(v.getName())) {
    				String pkgName = new String(v.getStrValue());
    				Log.i("SMM.Event", "Setting Event " + eventName +
    					" destination pkg to " + pkgName);
    				i.setPackage(pkgName);
				continue;
			    }
			    v.addToIntent(i);
            }
		}
		
		return i;
	}
	
	public Event addVar(EventVar var)
	{
		vars.put(var.getName(), var);
		return this;
	}
	
	public Boolean varsEqual(Event ev)
	{
		return ev.vars.equals(vars);
	}
	
	private void send(DataOutputStream out) throws IOException
	{
		out.writeInt(eventName.length());
		out.write(eventName.getBytes());
		out.writeInt(vars.size());
		for (EventVar v : vars.values())
			v.send(out);
		out.flush();
	}
	
	private void send(OutputStream out) throws IOException
	{
		DataOutputStream dOut = new DataOutputStream(out);
		
		send(dOut);
	}
	
	public byte[] toByteArray() throws IOException
	{
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		
		send(bStream);
		return bStream.toByteArray();
	}
	
	public String getName()
	{
		return eventName;
	}
	
	public EventVar getVar(String name) throws Exception
	{
		EventVar ret = vars.get(name);
		if (ret == null)
			throw new Exception("Variable name " + name + " not found");
		return ret;
	}
	
	public int getVarValue(String name)
	{
		try {
			return getVar(name).getValue();
		} catch (Exception e) {
			Log.e("SMM.Event", "Error getting var value:" + e.getMessage());
			return 0;
		}
	}

	public byte[] getVarStrValue(String name)
	{
		try {
			return getVar(name).getStrValue();
		} catch (Exception e) {
			Log.e("SMM.Event", "Error getting var str_value:" + e.getMessage());
			return null;
		}
	}

	public byte[] getVarBinValue(String name)
	{
		try {
			return getVar(name).getStrValue();
		} catch (Exception e) {
			Log.e("SMM.Event", "Error getting var binary value:" + e.getMessage());
			return null;
		}
	}	
	public Collection<EventVar> getVars()
	{
		return vars.values();
	}
	
	public int varsCount()
	{
		return vars.size();
	}

	/** the hash code of the class is only the msg_id, 
	 * without variables (if the class contains any) */
	@Override
	public int hashCode() {
		return eventName.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return o.hashCode() == hashCode();
	}
}
