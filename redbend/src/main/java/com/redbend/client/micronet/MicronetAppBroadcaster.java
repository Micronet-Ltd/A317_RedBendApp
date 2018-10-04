/////////////////////////////////////////////////////////////
// MicronetAppBroadcaster:
//  Sends broadcast messages to other applications

// Changes Required to Client:
//      Add this function to clientservice.java

/////////////////////////////////////////////////////////////
/**
 * Created by dschmidt on 8/26/16.
 */

package com.redbend.client.micronet;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.client.BuildConfig;


public class MicronetAppBroadcaster {

    public static final String TAG = "RBC-Broadcaster";


    public static final String BROADCAST_STARTING = "com.redbend.client.micronet.STARTING";
    public static final String EXTRA_VERSION = "VERSION";


    //////////////////////////////////////////////////////
    // sendStartingBroadcast()
    //  sends a broadcast when to inform other applications what version is running
    //  called by client service on startup
    //////////////////////////////////////////////////////
    public static void sendStartingBroadcast(Context context) {

        Log.d(TAG, "broadcasting version " + BuildConfig.VERSION_NAME + " to apps");

        Intent intent = new Intent(BROADCAST_STARTING);
        intent.putExtra(EXTRA_VERSION, BuildConfig.VERSION_NAME);

        context.sendBroadcast(intent);
    }

}
