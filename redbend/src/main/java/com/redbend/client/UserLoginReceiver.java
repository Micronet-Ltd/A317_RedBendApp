package com.redbend.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.SmmReceive;

/*	
 * UserLoginReceiver - When the user logs into Android - initiate the unlock BL
 * Listening to android.intent.action.USER_PRESENT
 */

public class UserLoginReceiver extends SmmReceive
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i(LOG_TAG, "+onReceive");
		sendEvent(context, ClientService.class, new Event("D2B_USER_LOGGED_IN"));
	}
}
