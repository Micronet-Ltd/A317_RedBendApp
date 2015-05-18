/*
 *******************************************************************************
 *
 * EventHandlerContext.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

/**
 * Event handler execute
 */
public interface EventHandlerContext {
	
/**
 * Execute the event handler.
 *
 * @param	handler		The event handler
 * @ev		ev			The event
 */
	public void exec(EventHandler handler, Event ev, int flowId, int uiMode);
}
