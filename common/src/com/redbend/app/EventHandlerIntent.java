/*
 *******************************************************************************
 *
 * EventHandlerIntent.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.IOException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Event handler intent
 */
public class EventHandlerIntent extends EventHandler
{
	public static final int HANDLER_INTENT_FLAG_NO_BACKGROUND = 1;
	
	private ComponentName name;
	private boolean noBackground = false; 
	
	/**
	 * Constructor
	 */
	public EventHandlerIntent(Context ctx, Class<? extends DilActivity> cls)
	{
		super(ctx);
		name = new ComponentName(ctx.getApplicationContext(), cls);
	}
	
	public EventHandlerIntent(Context ctx, Class<? extends DilActivity> cls, int flags)
	{
		super(ctx);
		name = new ComponentName(ctx.getApplicationContext(), cls);
		
		if ((flags & HANDLER_INTENT_FLAG_NO_BACKGROUND) != 0)
			noBackground = true;
	}

	/**
	 * Create event handler intent.
	 *
	 * @param	ev		The event
	 *
	 * @return	The intent
	 */
	@Override
	protected Intent activityHandler(Event ev)
	{
		Intent i;

		try {
			Log.d("SMM.Intent", "Posting intent for activity " + name.getClassName() +
					" for event " + ev.getName());
			i = new Intent();
			i.setComponent(name);
			i.putExtra(DilActivity.eventMsg, ev.toByteArray());
			if (noBackground)
				i.putExtra(DilActivity.noBackgroundExtra, true);
			return i;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
