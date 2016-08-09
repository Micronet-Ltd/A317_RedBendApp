/*
 *******************************************************************************
 *
 * ExtNodesHandler.java
 *
 * Handles read/write/exec to DM Tree nodes stored in external storage.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.lang.annotation.*;
import java.lang.reflect.Method;

import android.util.Log;

public class ExtNodesHandler {
	private static final String LOG_TAG = "ExtNodesHandler";
	
	private final static int VDM_ERR_OK = 0x0000;
	private final static int VDM_ERR_UNSPECIFIC = 0x0010;
	private final static int VDM_ERR_EXEC_START_RANGE = 0x3000;
	private final static int VDM_ERR_POSTPONE = VDM_ERR_EXEC_START_RANGE + 200;
	
	private native void initExt();
	private native void termExt();
	private native boolean registerRead(String uri, int id);
    private native boolean registerWrite(String uri, int id);
    private native boolean registerExec(String uri, int id);
    
	public static enum FuncType {
		READ,
		WRITE,
		EXEC
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface uri {
	    String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface type {
	    FuncType value() default FuncType.READ;
	}
    
	/** keeps the methods of the external callbacks implementation */
	private Method methods[];
	
    /** called through JNI, when an external storage node read is called */
    protected byte []read(int id, int offset)
    {
    	Log.d(LOG_TAG, "read(): id: " + id + " offset: " + offset);
    	
    	/* in case no one registered, we will receive '0' as an ID */
    	if (id == 0)
    		return null;
    	
    	try {
			return (byte[]) methods[id-1].invoke(null, offset);
    	}
    	catch (Exception e) {
    		Log.d(LOG_TAG, "Failed to invoke MO handler method - " + 
    				methods[id-1].getName());
    		return null;
    	}
    }
    
    /** called through JNI, when an external storage node write is called */
    protected int write(int id, int offset, byte[] data, int totalSize)
    {
    	Log.d(LOG_TAG, "write(): id: " + id + " offset: " + offset + 
    		" total size " + totalSize);

    	/* in case no one registered, we will receive '0' as an ID */
    	if (id == 0)
    		return VDM_ERR_UNSPECIFIC;
    	
    	try {
    		return ((Boolean)methods[id-1].invoke(null, offset, data, totalSize)).booleanValue()
    		? VDM_ERR_OK : VDM_ERR_UNSPECIFIC;
    	}
    	catch (Exception e) {
    		Log.d(LOG_TAG, "Failed to invoke MO handler method - " + 
    				methods[id-1].getName());
    		return VDM_ERR_UNSPECIFIC;
    	}
    }
    
    /** called through JNI, for an external exec node */
    protected int exec(int id, byte[] data, String correlator)
    {
    	Log.d(LOG_TAG, "exec(): id: " + id + " correlator " + correlator);
    	try {
    		return ((Boolean)methods[id-1].invoke(null, data, correlator)).booleanValue() ? 
    			VDM_ERR_OK : VDM_ERR_POSTPONE;
    	}
    	catch (Exception e) {
    		Log.d(LOG_TAG, "Failed to invoke MO handler method - " + 
    				methods[id-1].getName());
    		return VDM_ERR_UNSPECIFIC;
    	}
    }
   
    public ExtNodesHandler(Class<?> extMethods)
    {
    	int i;
    	
    	methods = extMethods.getMethods();
    	initExt();
    	Log.d(LOG_TAG, "Getting register methods, received " + methods.length);
    	
    	for (i = 0; i < methods.length; i++)
		{
    		FuncType t;
			
			if (!methods[i].isAnnotationPresent(uri.class))
			{
				Log.d(LOG_TAG, "Method " + methods[i].getName() + " doesn't have uri defined");
				continue;
			}
			
			String uri = methods[i].getAnnotation(uri.class).value();
			
			if (methods[i].isAnnotationPresent(type.class))
				t = methods[i].getAnnotation(type.class).value();
			else
			{
				/* if no type annotation is defined, it is assumed to be read */
				t = FuncType.READ;
			}
			
			/* adding 1 to the id, because it could be zero, and zero means 
			 * unregister, so here we add, and when we are called we subtract 1 */
			
			switch(t) {
			case READ:
				Log.d(LOG_TAG, "Register read func " + methods[i].getName() +
						", with URI \"" + uri + "\" with id " + (i + 1));
				registerRead(uri, i + 1);
				break;
			case WRITE:
				Log.d(LOG_TAG, "Register write func " + methods[i].getName() +
						", with URI \"" + uri + "\" with id " + (i + 1));
				registerWrite(uri, i + 1);
				break;
			case EXEC:
				Log.d(LOG_TAG, "Register exec func " + methods[i].getName() +
						", with URI \"" + uri + "\" with id " + (i + 1));
				registerExec(uri, i + 1);
				break;
			default:
				break;
			}
		}
    }
    
    public void terminate()
    {
		for (Method m : methods)
		{
			FuncType t;
			
			if (!m.isAnnotationPresent(uri.class))
				continue;
			
			String uri = m.getAnnotation(uri.class).value();
			
			if (m.isAnnotationPresent(type.class))
				t = m.getAnnotation(type.class).value();
			else
			{
				/* if no type annotation is defined, it is assumed to be read */
				t = FuncType.READ;
			}
			
			switch(t) {
			case READ:
				registerRead(uri, 0);
				break;
			case WRITE:
				registerWrite(uri, 0);
				break;
			case EXEC:
				registerExec(uri, 0);
				break;
			default:
				break;
			}
		}
    	
    	termExt();
    }
}
