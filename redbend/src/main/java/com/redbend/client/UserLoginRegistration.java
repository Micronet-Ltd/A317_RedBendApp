package com.redbend.client;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;

public class UserLoginRegistration extends EventHandler{
	
	private static final String LOG_TAG="UserLoginRegistration";
	private static int REGISTER_USER_PRESENT = 1;
	private UserLoginReceiver m_userLoginReceiver;
	
	public UserLoginRegistration(Context ctx) {
		super(ctx);
		m_userLoginReceiver = new UserLoginReceiver();
	}
	
	@Override
	protected void genericHandler(Event ev) {
		try {
			int register = ev.getVarValue("DMA_VAR_LISTEN_TO_LOGIN_EVENT_ENABLE");
			if (register == REGISTER_USER_PRESENT) {
				ctx.registerReceiver(m_userLoginReceiver, new IntentFilter(
					"android.intent.action.USER_PRESENT"));
				Log.d(LOG_TAG, "Successfully registered to android.intent.action.USER_PRESENT");
			}
			else {
				ctx.unregisterReceiver(m_userLoginReceiver);
				Log.d(LOG_TAG, "Successfully Unregistered from android.intent.action.USER_PRESENT");
			}
		}catch(Exception e){
			Log.w(LOG_TAG, "failes to read variable DMA_VAR_LISTEN_TO_LOGIN_EVENT_ENABLE");
		}
	}
}
