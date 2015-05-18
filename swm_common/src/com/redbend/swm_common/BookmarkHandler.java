/*
 *******************************************************************************
 *
 * BookmarkHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.swm_common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.net.Uri;

import com.redbend.app.Event;
import com.redbend.app.EventHandler;

public class BookmarkHandler extends EventHandler {

	private static final String LOG_TAG = BookmarkHandler.class.getSimpleName();
	private static final String ACTION_INSTALL_SHORTCUT =
			"com.android.launcher.action.INSTALL_SHORTCUT";
	private static final String ACTION_UNINSTALL_SHORTCUT =
			"com.android.launcher.action.UNINSTALL_SHORTCUT";
	public static final String APPS_BOOKMARK = "apps_bookmark";
	public static final String BOOKMARK_NAME = "bookmark_name";
	public static final String BOOKMARK_URL = "bookmark_url";
	
	public BookmarkHandler(Context ctx) {
		super(ctx);
	}
	
	private static String getStringVal(Event ev, String varName) {
		byte[] byteVal;
					
		byteVal = ev.getVarStrValue(varName);
		
		if (byteVal == null) {
			return null;
		}
		
		String val = new String(byteVal);
		if (val.equals("NONE")) {
			return null;
		}
		return val;
	}
	
	// http://stackoverflow.com/questions/2833956/how-to-unescape-xml-in-java
	private static Map<String,String> buildBuiltinXMLEntityMap()
	{
	    Map<String,String> entities = new HashMap<String,String>(10);
	    entities.put( "lt", "<" );
	    entities.put( "gt", ">" );
	    entities.put( "amp", "&" );
	    entities.put( "apos", "'" );
	    entities.put( "quot", "\"" );
	    return entities;
	}
	
	public static String unescapeXML( final String xml )
	{
	    Pattern xmlEntityRegex = Pattern.compile( "&(#?)([^;]+);" );
	    //Unfortunately, Matcher requires a StringBuffer instead of a StringBuilder
	    StringBuffer unescapedOutput = new StringBuffer( xml.length() );

	    Matcher m = xmlEntityRegex.matcher( xml );
	    Map<String,String> builtinEntities = null;
	    String entity;
	    String hashmark;
	    String ent;
	    int code;
	    while ( m.find() ) {
	        ent = m.group(2);
	        hashmark = m.group(1);
	        if ( (hashmark != null) && (hashmark.length() > 0) ) {
	            code = Integer.parseInt( ent );
	            entity = Character.toString( (char) code );
	        } else {
	            //must be a non-numerical entity
	            if ( builtinEntities == null ) {
	                builtinEntities = buildBuiltinXMLEntityMap();
	            }
	            entity = builtinEntities.get( ent );
	            if ( entity == null ) {
	                //not a known entity - ignore it
	                entity = "&" + ent + ';';
	            }
	        }
	        m.appendReplacement( unescapedOutput, entity );
	    }
	    m.appendTail( unescapedOutput );

	    return unescapedOutput.toString();
	}
	
	public static Intent createBrowserIntent(String url) {
		String escapedBookmark = unescapeXML(url);
		Uri uri = Uri.parse(escapedBookmark);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW);
		browserIntent.setDataAndType(uri, "text/html");
		browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
		
		return browserIntent;
	}

	public static Intent createShortcutIntent(Context ctx, Intent browserIntent, String bookmarkName, boolean install) {
		Intent shortcutIntent = new Intent();
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, browserIntent);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, bookmarkName);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, 
			Intent.ShortcutIconResource.fromContext(ctx, R.drawable.default_bookmark_icon_144));

		if (install) {
			shortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
			shortcutIntent.putExtra("duplicate", false);
		}
		else {
			shortcutIntent.putExtra("duplicate", true);
			shortcutIntent.setAction(ACTION_UNINSTALL_SHORTCUT);
		}
		
		return shortcutIntent;
	}

	@Override
	protected void genericHandler(Event ev) {
		// build the data from the event
		Log.d(LOG_TAG, "BookmarkHandler received EVENT: " + ev.getName());
		String bookmarkName = getStringVal(ev, "BOOKMARK_NAME");
		String bookmarkUrl  = getStringVal(ev, "BOOKMARK_URL");
		if (bookmarkName == null || bookmarkUrl == null) {
			Log.d(LOG_TAG, "Bad event variables, bookmark name is: " + 
				bookmarkName + "and bookmark URL is: " + bookmarkUrl);
			return;
		}
				
		SharedPreferences prefs = ctx.getSharedPreferences(APPS_BOOKMARK, 0);
		SharedPreferences.Editor editor = prefs.edit();

		// build the browser intent
		Intent browserIntent = createBrowserIntent(bookmarkUrl);
		Log.d(LOG_TAG, "BookmarkHandler shortcutIntent: " + browserIntent);
		
		Intent shortcutIntent = null;
		// build the shortcut intent
		if (ev.getName().equals("DMA_MSG_ENROLL_PUT_BOOKMARK")) {
			editor.putString(BOOKMARK_NAME, bookmarkName);
			editor.putString(BOOKMARK_URL, bookmarkUrl);
			editor.commit();

			shortcutIntent = createShortcutIntent(ctx, browserIntent, bookmarkName, true);	
			Log.d(LOG_TAG, "PUT BOOKMARK request from ENROLL SM");
		}
		else if (ev.getName().equals("DMA_MSG_ENROLL_REMOVE_BOOKMARK")) {
			editor.remove(BOOKMARK_NAME);
			editor.remove(BOOKMARK_URL);
			editor.commit();
		
			shortcutIntent = createShortcutIntent(ctx, browserIntent, bookmarkName, false);
			Log.d(LOG_TAG, "REMOVE BOOKMARK request from ENROLL SM");
		}
		else
			Log.e(LOG_TAG, "undefined request " + ev.getName());
		
		if (shortcutIntent != null)
		{
			Log.d(LOG_TAG, "BookmarkHandler sending broadcast: " + shortcutIntent);
			ctx.sendBroadcast(shortcutIntent);
		}	
	}
}
