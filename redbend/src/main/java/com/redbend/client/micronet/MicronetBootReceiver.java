/////////////////////////////////////////////////////////////
// BootReceiver:
//  Receives the Boot Message from the System

// Changes Required to Client:
//      Add this receiver to Manifest.xml


/////////////////////////////////////////////////////////////
package com.redbend.client.micronet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


// This is registered in the Manifest
public class MicronetBootReceiver extends BroadcastReceiver {

    // Here is where we are receiving our boot message.
    //  Information about this should also be put in the manifest.

    public static final String TAG = "RBC-MNBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "System Boot Notification");


        // Clear all prior locks from previous sessions

        // start the binding service if it was not already started, and clear out locks from prior boots

        Intent i = new Intent(context, MicronetBindingService.class);
        i.setAction(MicronetBindingService.START_ACTION_CLEAR_INSTALL_LOCKS);
        context.startService(i);

    }

} // class
