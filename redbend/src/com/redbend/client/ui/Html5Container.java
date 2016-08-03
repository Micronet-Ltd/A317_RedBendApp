/*
 *******************************************************************************
 *
 * Html5Container.java
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import com.redbend.client.R;
import com.redbend.client.RbAnalyticsHelper;

import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.annotation.SuppressLint;
import com.redbend.app.DilActivity;
import com.redbend.app.Event;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/*
 * This Class runs the Automotive application through HTML5 over WebView.
 * it parse the events received from the smm and send it to the js through interface.  
 * all the relevant messages have to be registered in the ClientService.java.
 */
public class Html5Container extends DilActivity {
	public static String TAG = "StartupAutomotive";
	WebView m_wv = null;
	private boolean m_pageFinished;
	private Vector<Event> m_eventsQueue;
	
	
	/*
	 * This method takes the event message and pharse it to a websocket message.
	 * then it send the message through an interface to the javascript layer. 
	 */
	private void sendEventToJs(Event ev)
	{
		Log.d(LOG_TAG, "+sendEventToJs");
		try {
			byte[] arr = ev.toByteArray();
			int evLen = arr.length;
			String toJs = String.valueOf((evLen & 0xff000000)>>24) +","+
					String.valueOf((evLen & 0xff0000)>>16) +","+
					String.valueOf((evLen & 0xff00)>>8) +","+
					String.valueOf((evLen & 0xff)  >>0) +"," ;
			for (int ind = 0 ; ind < arr.length ; ind++)
			{
				toJs = toJs + String.valueOf(arr[ind]);
				if (ind+1 < arr.length)
					toJs = toJs + ",";
			}
			m_wv.loadUrl("javascript:parseUiEvent('"+toJs+"')");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * This method called in the beggining of the application, 
	 * and create the webview that runs the html5.  
	 */
	@SuppressLint({ "NewApi", "SetJavaScriptEnabled" })
	@Override
	protected void setActiveView(boolean start, Event ev) {
	
		Log.d(LOG_TAG, "setActiveView is called");
		setContentView(R.layout.activity_main);

		m_wv = (WebView) findViewById(R.id.webview);
		WebSettings webSettings = m_wv.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		
		if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) 
			webSettings.setAllowUniversalAccessFromFileURLs(true);
		
		m_eventsQueue = new Vector<Event>();
		m_eventsQueue.add(ev);
		m_wv.setWebViewClient(new WebViewClient(){
			/*
			 * when the page was finished loading, 
			 * this method pops all the events send to the html5 DIL,
			 * while the page was loading. 
			 */
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				Log.d(LOG_TAG,"+onPageFinished");
				for (Event event: m_eventsQueue)
					sendEventToJs(event);
				m_eventsQueue.clear();
				m_pageFinished = true;
				
				Log.d(LOG_TAG, "-onPageFinished");
			}
		});

		JavaScriptInterface jsInterface = new JavaScriptInterface(this);
		m_wv.addJavascriptInterface(jsInterface, "JSInterface");
		
		m_wv.loadUrl("file:///android_asset/html5/index.html");
		// save our activity
		Log.d(LOG_TAG, "-setActiveView");
	}
	
	/* called on receiving a new Event for the same intent */
	@Override
	protected void newEvent(Event receivedEvent) {
		Log.d(LOG_TAG, "Received event " + receivedEvent.getName() + " m_pageFinished:" + m_pageFinished);
		//If page wasn't finished loading - add the event to the queue - it will be sent later
		if (!m_pageFinished)
		{
			Log.d(LOG_TAG, "newEvent::!m_pageFinished");
			//Check if the event was already added to the array
			if (!m_eventsQueue.contains(receivedEvent)){
				Log.d(LOG_TAG, "newEvent::!m_eventsQueue.contains(receivedEvent)");
				m_eventsQueue.add(receivedEvent);
			}
		}
		else{
			Log.d(LOG_TAG, "newEvent::sendEventToJs");
			sendEventToJs(receivedEvent);
		}
	}
	
	/*
	 * This class is the interface between the javascript and the android layer.
	 * through it the javascript layer can invoke methods in the android layer.
	 */
	public class JavaScriptInterface {
	    private Html5Container activity;

	    public JavaScriptInterface(Html5Container activiy) {
	        this.activity = activiy;
	    }

	    /*
	     * This method is called from the html5 and finish the UI
	     */
	    @JavascriptInterface
	    public void finishApp(){
	        activity.finish();
	    }
	    
	    /*
	     * This method received the event from the javascript layer and send it to the BL.
	     */
	    @JavascriptInterface
	    public void sendEventToBl(byte[] arr){
	    	Log.d(LOG_TAG, "+sendEventToBl");
	    	try {
	    		//Don't need the first 4 bytes, remove them, and send the buffer
	    		byte [] subArray = Arrays.copyOfRange(arr, 4,  arr.length);// didn't find a smarter way to remove 4 bytes :(
	    		Event event = new Event(subArray);
	    		sendEvent(event);
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    /*
	     * This method sets state of Analytics
	     */
	    @JavascriptInterface
	    public void setApplicationAnalyticsState(int state){
	    	Log.d(LOG_TAG, "+setApplicationAnalyticsState state:"+state);
	    	RbAnalyticsHelper.setRbAnalyticsServiceState(getApplicationContext(), state==1);
	    }
	    
	    // Return if RB Analytics is running or not
	    @JavascriptInterface
	    public  int isRbAnaliticsRunning()
		{
			int analyticsState = 
					RbAnalyticsHelper.isRbAnaliticsRunning(getApplicationContext()) ? 1:0;
			return analyticsState;
		}
	}
}
