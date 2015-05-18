/*
 *******************************************************************************
 *
 * EventVar.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.content.Intent;
import android.os.Bundle;

public class EventVar {
	
	/* these are the variable types, which are transmitted
	 * through IPC */
	/* XXX Java doesn't correctly support unsigned int,
	 * VAR_TYPE_INT is in fact unsigned in SMM */
	/* XXX All integers sent from Android DIL are signed by definition,
	 * but are sent as "unsigned" type */
	public static final int VAR_TYPE_INT = 1;
	public static final int VAR_TYPE_STR = 2;
	public static final int VAR_TYPE_BIN = 3;
	
	private String name;
	private int value;
	private int type;
	private byte data[];
		
	public EventVar(String name, int value) 
	{
		this.name = name;
		this.value = value;
		type = VAR_TYPE_INT;
	}
	
	/* right now no data type var could be created */
	public EventVar(String name, byte[] data) 
	{
		this.name = name;
		this.data = data;
		type = VAR_TYPE_BIN;
	}

	public EventVar(String name, String str) 
	{
		this(name, (str != null) ? str.getBytes() : null);
		type = VAR_TYPE_STR;
	}

	public EventVar(DataInputStream in) throws IOException
	{
		name = new String(Event.readString(in));
		type = in.readInt();
		switch (type) {
		case VAR_TYPE_INT:
		case 4: /* XXX Java doesn't correctly support unsigned int,
				 * this is only a temporary workaround */
			value = in.readInt();
			break;
		/* in Java we are using byte array, which is not
		 * zero terminated, so there's no difference between
		 * string and data */
		case VAR_TYPE_STR:
		case VAR_TYPE_BIN:
			data = Event.readString(in);
			break;
		default:
			throw new IOException("Invalid type " + type + " received, when reading an event variable");
		}
	}
	
	public EventVar(String name, Bundle b)
	{
		Object content = b.get(name);
		
		this.name = name;
		if (content instanceof String) {
			type = VAR_TYPE_STR;
			data = ((String) content).getBytes();
		}
		else if (content instanceof byte[]) {
			type = VAR_TYPE_BIN;
			data = (byte[]) content;  
		}
		else { // integer
			type = VAR_TYPE_INT;
			value = (Integer) content;
		}
	}
	
	public void addToIntent(Intent i)
	{
		switch (type) {
		case VAR_TYPE_INT:
			i.putExtra(name, value);
			break;
		case VAR_TYPE_BIN:
			i.putExtra(name, data);
			break;
		default: /* VAR_TYPE_STR */
			i.putExtra(name, data != null ? new String(data) : "");
			break;
		}
	}

	public String getName()
	{
		return name;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public byte[] getStrValue()
	{
		return data;
	}
	
	public void send(DataOutputStream out) throws IOException
	{
		out.writeInt(name.length());
		out.write(name.getBytes());
		out.writeInt(type);
		switch (type) {
		case VAR_TYPE_INT:
			out.writeInt(value);
			break;
		case VAR_TYPE_STR:
		case VAR_TYPE_BIN:
			/* if no data or zero-length data, then no data to send,
			 * send just the zero length */
			if (data == null || data.length == 0) {
				out.writeInt(0);
				break;
			}
			out.writeInt(data.length);
			out.write(data);
			break;
		default:
			/* shouldn't happen */
			break;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EventVar))
				return super.equals(o);
		
		EventVar varO = (EventVar) o;
		
		if (!varO.name.equals(name) || varO.type != type)
			return false;
		
		switch (type) {
		case VAR_TYPE_INT:
			return varO.value == value;
		case VAR_TYPE_STR:
		case VAR_TYPE_BIN:
			if (varO.data == null && data == null)
				return true;
			else if (varO.data == null || data == null)
				return false;
			
			/* if there's data, then both should have the same data */
			return Arrays.equals(varO.data, data);
		default:
				return false;
		}
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
