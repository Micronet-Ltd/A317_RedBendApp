/*
 *******************************************************************************
 *
 * EmptyStartupActivity.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client;

public class AdminRequestActivity extends SwmStartupActivityBase {            
        
	@Override 
	protected void sendStartServiceEvent(){	
	    //Do not start in this Activity.
	}
	
	@Override
	protected void userAcceptedPermission(){
            //Do nothing in case of user declined
	}
	
	@Override 
	protected void userDeclinedPermission(){
	    //Do nothing in case of user declined
	}
}
