///////////////////////////////////////////////
/**
 * Created by dschmidt on 7/27/16.
 */
// Contains code for handling micronet-specific RPC to this redbend client app
// This is called to create or release the Installation locks.
// Can be bound or can be started with intents.

// Changes Required to Client:
//      Add this service & the service intents to Manifest.xml
//      Add a permission for access to this service to Manifest.xml

///////////////////////////////////////////////


package com.redbend.client.micronet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.redbend.client.BuildConfig;
import com.redbend.client.micronet.IMicronetRBClientAidlInterface;

import java.util.UUID;


public class MicronetBindingService extends Service {

    public static final String TAG = "RBC-MNBindingService";


    // Start Actions:
    public static final String START_ACTION_PING = "com.redbend.client.micronet.PING";
    public static final String START_ACTION_CLEAR_INSTALL_LOCKS = "com.redbend.client.micronet.CLEAR_INSTALL_LOCKS";
    public static final String START_ACTION_ACQUIRE_INSTALL_LOCK = "com.redbend.client.micronet.ACQUIRE_INSTALL_LOCK";
    public static final String START_ACTION_RELEASE_INSTALL_LOCK = "com.redbend.client.micronet.RELEASE_INSTALL_LOCK";


    public static final String START_ACTION_SET_ROAMING_MODE = "com.redbend.client.micronet.SET_ROAMING_MODE";

    // Extras for Actions
    public static final String START_EXTRA_LOCK_NAME = "LOCK_NAME";
    public static final String START_EXTRA_SECONDS = "SECONDS";


    public static final String START_EXTRA_ALLOW_ROAMING = "ALLOW_ROAMING"; // boolean



    // Reply Actions
    public static final String START_ACTION_REPLY_PING = "com.redbend.client.micronet.REPLY_PING";


    // Extras for Reply Actions
    public static final String START_EXTRA_VERSION = "VERSION";


    // uniqueSessionID
    //   assign all locks to a session ID so we know if they were created during this instance of the service
    //   when we receive the boot indicator, we can delete all locks that were not created in this same instance.
    String  uniqueSessionID = null; // an ID to apply to all locks for this session.

    public MicronetBindingService() {

        if (uniqueSessionID == null)
            uniqueSessionID = UUID.randomUUID().toString();

    }

    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;

        //throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (intent != null) {
            String action = intent.getAction();

            if (action.equals(START_ACTION_PING)) {
                Log.d(TAG, "Received Ping, Sending Reply: version " + BuildConfig.VERSION_NAME);

                Intent replyIntent = new Intent(START_ACTION_REPLY_PING);
                replyIntent.putExtra(START_EXTRA_VERSION, BuildConfig.VERSION_NAME);

                sendBroadcast(replyIntent);

            } else
            if (action.equals(START_ACTION_SET_ROAMING_MODE)) {
                boolean allowRoaming = intent.getBooleanExtra(START_EXTRA_ALLOW_ROAMING, false);

                setRoamingAllowed(allowRoaming);
            } else
            if (action.equals(START_ACTION_CLEAR_INSTALL_LOCKS)) {

                // clear locks from prior boots.
                int num_cleared = clearPriorInstallLocks();

                if (num_cleared >=0) {
                    Log.d(TAG, "Cleared " + num_cleared + " Installation Locks from prior sessions");
                }

            } else
            if (action.equals(START_ACTION_ACQUIRE_INSTALL_LOCK)) {

                String lock_name = intent.getStringExtra(START_EXTRA_LOCK_NAME);
                int expires_seconds = intent.getIntExtra(START_EXTRA_SECONDS, 24*60*60);

                acquireLock(0, lock_name, expires_seconds);

            }
            else if (action.equals(START_ACTION_RELEASE_INSTALL_LOCK)) {

                String lock_name = intent.getStringExtra(START_EXTRA_LOCK_NAME);

                releaseLock(0, lock_name);

            }

        }

        return START_NOT_STICKY;
    }

    /////////////////////////////////////////////////
    // clearPriorInstallLocks()
    //  removes all installation locks that were set by a different load of this service.
    //  used at boot time to clear any locks that were set in a different boot instance.
    //  Note we cannot just clear out all locks because some may have been set by apps earlier in this boot sequence.
    /////////////////////////////////////////////////
    private int clearPriorInstallLocks() {

        Log.d(TAG, "Clearing old Installation Locks");

        Context context = getApplicationContext();

        MicronetMySQLiteHelper dbHelper = new MicronetMySQLiteHelper(context);

        int res = dbHelper.removeOtherLocks(uniqueSessionID);

        dbHelper.close();

        return res;
    } // clearPriorInstallLocks()

    /////////////////////////////////////////////////
    // acquireLock
    //  add a Lock to prevent installation of FOTAs
    /////////////////////////////////////////////////
    private int acquireLock(int userid, String lock_name, int expires_seconds) {


        long elapsed_now_s = SystemClock.elapsedRealtime() / 1000;
        long elapsed_expires_s = elapsed_now_s + expires_seconds;


        Log.d(TAG, "Creating Installation Lock " + (lock_name != null ? lock_name : "") + " for UID " + userid + " for " + expires_seconds + " seconds (until + " + elapsed_expires_s +" s).");

        if ((lock_name == null) || (lock_name.isEmpty())) {
            Log.e(TAG, " Error. Locks must be named. Refusing Lock Request.");
            return -1;
        }

        if (expires_seconds > 24*60*60) {
            Log.e(TAG, " Error. Locks cannot be over 24 hours. Refusing Lock Request " + lock_name + " for UID " + userid + " for " +expires_seconds + " seconds.");
            return -1;
        }


        Context context = getApplicationContext();

        MicronetMySQLiteHelper dbHelper = new MicronetMySQLiteHelper(context);

        int res = dbHelper.addorUpdateLock(uniqueSessionID, userid, lock_name, elapsed_expires_s);

        dbHelper.close();

        if (res < 0) {
            Log.e(TAG, "Unable to add Installation Lock!");

        }

        return res;

    }



    /////////////////////////////////////////////////
    // releaseLock
    //  remove a Lock that prevented installation of FOTAs
    /////////////////////////////////////////////////
    private int releaseLock(int userid, String lock_name) {

        Log.d(TAG, "Releasing Installation Lock " + (lock_name != null ? lock_name : "") + " for UID " + userid);

        if ((lock_name == null) || (lock_name.isEmpty())) {
            Log.e(TAG, " Error. Locks must be named. Refusing Lock Request.");
            return -1;

        }


        Context context = getApplicationContext();

        MicronetMySQLiteHelper dbHelper = new MicronetMySQLiteHelper(context);
        int res = dbHelper.removeLock(userid, lock_name);
        dbHelper.close();

        if (res < 0) {
            Log.d(TAG, "Unable to find or remove lock with UID " + userid + " name = " + lock_name);
        }

        return res;
    }





    private int setRoamingAllowed(boolean allowRoaming) {
        if (allowRoaming) {
            MicronetRoaming.setRoamingMode(this, MicronetRoaming.MODE_IGNORE_ROAMING);
        } else {
            MicronetRoaming.setRoamingMode(this, MicronetRoaming.MODE_RESPECT_ROAMING);
        }
        return 0;
    } // setRoaming










    /**
     * IAdd definition is below
     */
    private final IMicronetRBClientAidlInterface.Stub mBinder = new IMicronetRBClientAidlInterface.Stub() {

        ////////////////////////////////////////////////
        // getNoInstallLock()
        //  returns 0 if the lock was created
        ////////////////////////////////////////////////
        @Override
        public int getNoInstallLock(String lock_name, int expires_seconds) throws RemoteException {

            int uid = Binder.getCallingUid();
            return acquireLock(uid, lock_name, expires_seconds);

        }

        ////////////////////////////////////////////////
        // releaseNoInstallLock()
        //  returns 0 if the lock was found and released, -1 if it was not found.
        ////////////////////////////////////////////////
        @Override
        public int releaseNoInstallLock(String lock_name) throws RemoteException {

            int uid = Binder.getCallingUid();
            return releaseLock(uid, lock_name);

        }



        ////////////////////////////////////////////////
        // getVersion()
        //  gets the version of the client
        ////////////////////////////////////////////////
        @Override
        public String getVersion() throws RemoteException {

            return BuildConfig.VERSION_NAME;
        }


        ////////////////////////////////////////////////
        // setRoamingMode()
        //  returns 0 if it was set
        ////////////////////////////////////////////////
        @Override
        public int setRoamingMode(boolean allowRoaming) throws RemoteException {

            return setRoamingAllowed(allowRoaming);

        }


    };


} // class = MicronetRBClientService
