/*
 *******************************************************************************
 *
 * EventMultiplexer.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;
import android.os.PowerManager;
import android.util.Log;

public class EventMultiplexer {
	private final String LOG_TAG = "SMM.Multiplex";
	private final PowerManager.WakeLock mWl;
	
	private LinkedBlockingDeque<Event> eventQueue;
	private boolean processing = true;
	private Thread eventProcessing = new Thread(new Runnable() {
		public void run() {
			while (processing) {
				try {
					// this would block till there's event
					Event ev = eventQueue.take();
					queueEvent(ev);
					mWl.release();
				} catch (InterruptedException e) {
					// eventQueue.take could be interrupted then, wait again
					continue;
				}
			}
			eventQueue.clear();
		}
	});

	private class HandlerFilter {
		private EventHandler handler;
		private ArrayList<EventVar> filter;
		private int flowId;
		private int uiMode;
		
		public HandlerFilter(EventHandler h, int flow, Event e, int mode)
		{
			handler = h;
			flowId = flow;
			uiMode = mode;
			filter = new ArrayList<EventVar>(e.varsCount());
			
			for (EventVar var : e.getVars())
				filter.add(var);
		}
		
		public void handle(EventHandlerContext context, Event ev)
		{
			for (EventVar v : filter)
			{
				String varName = v.getName();
				
				try {			
					EventVar var = ev.getVar(varName);

					/* check that all filter variables have the same value */
					if (!var.equals(v))
					{
						Log.d(LOG_TAG, "Handler " + handler.getClass().getName() + 
								" doesn't match value of var ID " + varName +" value = " + var.getValue());
						Log.d(LOG_TAG, "Current var " + handler.getClass().getName() + 
								" doesn't match value of var ID " + v.getName() +" value = " + v.getValue());
						return;
					}
				} catch (Exception e) {
					/* if any of the values is not found then doesn't call the 
					 * handler */
					Log.d(LOG_TAG, "Handler " + handler.getClass().getName() + 
							" has filter of var ID " + varName + 
							" that doesn't exist in the received event");				
					return; 
				}
			}
			
			context.exec(handler, ev, flowId, uiMode);
		}

		public String getName()	{
			return handler.getClass().getSimpleName();
		}
	}
	
	HashMap<Event, ArrayList<HandlerFilter>> events;
	EventHandlerContext execContext;
	
	public EventMultiplexer(EventHandlerContext context, PowerManager.WakeLock wakeLock) {
		mWl = wakeLock;
		events = new HashMap<Event, ArrayList<HandlerFilter>>();
		execContext = context;
		eventQueue = new LinkedBlockingDeque<Event>();
		eventProcessing.start();
	}

	public void addEventHandler(int flowId, Event ev, int mode, EventHandler eh) {
		ArrayList<HandlerFilter> eventList = events.get(ev);
		HandlerFilter h = new HandlerFilter(eh, flowId, ev, mode);
		
		if (eventList == null)
		{
			eventList = new ArrayList<HandlerFilter>();
			events.put(ev, eventList);
		}
		eventList.add(h);
	}

	private void queueEvent(Event ev) {
		ArrayList<HandlerFilter> k = events.get(ev);
		
		if (k == null)
		{
			Log.d(LOG_TAG, "No handler registered for event " + ev.getName());
			return;
		}
		
		for (HandlerFilter h : k) {
			Log.d(LOG_TAG, h.getName() + " called for event " + ev.getName());
			h.handle(execContext, ev);
		}
	}

	public void handleEvent(Event ev) {
		if (!processing) {
			Log.e(LOG_TAG, "Event " + ev.getName() + " will never be handled");
			return;
		}
		mWl.acquire();
		eventQueue.add(ev);
	}

	public void destroy() {
		processing = false;
		eventProcessing.interrupt();
	}
}
