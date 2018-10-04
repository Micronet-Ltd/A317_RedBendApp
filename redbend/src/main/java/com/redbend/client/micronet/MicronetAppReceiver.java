/////////////////////////////////////////////////////////////
// AppReceiver:
//  Receives Messages from other applications on the system

// Other applications can send a message to this client to check for updates with the server,
//      so they don't have to wait until the next (7 hour) timeout

// Changes Required to Client:
//      Add this receiver to Manifest.xml


/////////////////////////////////////////////////////////////
package com.redbend.client.micronet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.SmmService;
import com.redbend.client.ClientService;

import java.io.IOException;


// This is registered in the Manifest
public class MicronetAppReceiver extends BroadcastReceiver {

    // Here is where we are receiving our message from other apps to check for updates
    //  Information about this should also be put in the manifest.

    public static final String TAG = "RBC-MNAppReceiver";

    private static final String ACTION_CHECK_FOR_UPDATES = "SwmClient.CHECK_FOR_UPDATES_NOW";
    private static final String ACTION_INSTALL_NOW = "com.redbend.client.micronet.INSTALL_NOW";


    private final static String USER_INITIATED_EVENT_NAME = "DMA_MSG_SESS_INITIATOR_USER_SCOMO";

    @Override
    public void onReceive(Context context, Intent intent) {




        // SwmClient.CHECK_FOR_UPDATES_NOW
        // Try to start a server session (poll the server)

        String action = intent.getAction();


        if (action.equals(ACTION_CHECK_FOR_UPDATES)) {
            // Tell the RBC lib to contact the server right away (override the internal download check timer)

            Log.d(TAG, "Notification received. Attempting Service Start: " + USER_INITIATED_EVENT_NAME);

            Intent userInitIntent = new Intent(context.getApplicationContext(), ClientService.class);

            try {
                byte[] eventBuffer = new Event(USER_INITIATED_EVENT_NAME).toByteArray();

                userInitIntent.putExtra(SmmService.flowIdExtra, 1);
                userInitIntent.putExtra(SmmService.startServiceMsgExtra, eventBuffer);
                userInitIntent.putExtra(SmmService.returnFromBackground, false);
                context.startService(userInitIntent);
            } catch (IOException e) {
                Log.e(TAG, "Exception trying to start service: " + e.toString());
            }

        } else
        if (action.equals(ACTION_INSTALL_NOW)) {

            Log.d(TAG, "Notification received: " + ACTION_INSTALL_NOW);
            // override any locks that are held so that any waiting (downloaded) installations will take place right away.
            //  Note: It may take up to a minute for an installation to begin.
            MicronetConfirmHandler.overrideLocksNow();
        } else {
            Log.e(TAG, "Unknown Action: " + action);
        }


    }

} // class
