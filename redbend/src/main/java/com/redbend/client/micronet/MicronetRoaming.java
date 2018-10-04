package com.redbend.client.micronet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.redbend.client.BuildConfig;

import com.redbend.client.StartupActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;


/**
 * Created by dschmidt on 10/25/17.
 */

public class MicronetRoaming extends ContextWrapper{

    public static final String TAG = "RBC-Roaming";



    private static final String FILENAME_BACKUP_PATH = "/internal_Storage/rbc";
    private static final String FILENAME_STANDARD_PATH = "/data/data/" + BuildConfig.APPLICATION_ID + "/shared_prefs";

    public static final String FILENAME_PREFS_KEY = "rbc_roaming";
    public static final String FILENAME_PREFS_FILE = FILENAME_PREFS_KEY + ".xml";



    // Operational modes for either respecting or ignoring the fact that we are roaming
    //  If we respect roaming then we will let the RBC library know when we are roaming (and it will prevent downloads
    //      during this time).
    //  If we ignore roaming then we will always tell the RBC library that we are NOT roaming, even if we are.


    // In order to save this info, we will store it in a file that is outside the /data/data directory so that it will
    //  not disappear when the client's data is cleared

    static final int MODE_RESPECT_ROAMING  = 1; // Do NOT allow roaming (Respect what the OS says). This is default.
    static final int MODE_IGNORE_ROAMING  = 2; // Allow roaming (Ignore what the OS says).


    static int roamingMode = MODE_RESPECT_ROAMING; // default is to NOT allow roaming


    static boolean isInitialized = false; // set to true once we are initialized

    public MicronetRoaming(Context base) {
        super(base);
    }

    //////////////////////////////////////////////////////////////////////
    // getInitialRoamingStatus()
    //  determine what we are going to tell the RBC initially about our roaming status after start-up
    //////////////////////////////////////////////////////////////////////
    public static void init(Context context) {
        // we may be in one of two modes:
        //      A) Respect Roaming -> tell RBC we are roaming until proven otherwise
        //      B) Ignore Roaming -> tell RBC we are not roaming since we will never let it know we are roaming


        // Initialize the roaming mode that we are starting in

        if (!fileExists(FILENAME_STANDARD_PATH, FILENAME_PREFS_FILE)) {
            // No standard pref .. was our data wiped ?
            if (fileExists(FILENAME_BACKUP_PATH, FILENAME_PREFS_FILE)) {
                // but we have one at the backup location
                if (areFilePermissionsOK(context, FILENAME_BACKUP_PATH, FILENAME_PREFS_FILE)) {
                    // this file looks like it was placed there by us, permissions are set correctly
                    copyFile(FILENAME_BACKUP_PATH, FILENAME_STANDARD_PATH, FILENAME_PREFS_FILE);
                }
            }
        }


        roamingMode = getRoamingMode(context);
        isInitialized = true;


        Log.i(TAG, "Initialized w/ Roaming Mode = " + roamingMode + " (" + getModeDescription(roamingMode) + ")" );

    } // init()


    //////////////////////////////////////////////////////////////////////
    // getAdjustedRoamingStatus()
    //  determine whether we are going to tell the RBC we are roaming
    // isReallyRoaming: the roaming status being reported by the phone
    // Returns: the roaming status to report to the RBC library
    //////////////////////////////////////////////////////////////////////
    public static boolean getAdjustedRoamingStatus(Context context, boolean isReallyRoaming) {


        // perform initialization if we are not already initialized
        if (!isInitialized) init(context);

        // we may be in one of two modes:
        //      A) Respect Roaming -> just use the isReallyRoaming status from the phone
        //      B) Ignore Roaming -> always return false to pretend we are not roaming

        switch (roamingMode) {

            default:
                Log.e(TAG, "getNewRoamingStatus(): Unknown roamingMode =" + roamingMode);
            case MODE_RESPECT_ROAMING:
                return isReallyRoaming;
            case MODE_IGNORE_ROAMING:
                return false;
        }

    } // getAdjustedRoamingStatus()


    //////////////////////////////////////////////////////////////////////
    // getModeDescription()
    //  get a description of the current mode (for logging)
    //////////////////////////////////////////////////////////////////////
    static String getModeDescription(int requestedRoamingMode) {

         return (requestedRoamingMode == MODE_IGNORE_ROAMING ? "Roaming-Allowed" : "Roaming-NotAllowed");
    }


    //////////////////////////////////////////////////////////////////////
    // changeRoamingMode()
    //////////////////////////////////////////////////////////////////////
    public static void setRoamingMode(Context context, int newRoamingMode) {


        Log.d(TAG, "Setting Roaming Mode to " + getModeDescription(newRoamingMode));

        SharedPreferences.Editor editor = context.getSharedPreferences(FILENAME_PREFS_KEY, context.MODE_PRIVATE).edit();
        editor.putInt("roamingMode", newRoamingMode);
        editor.commit(); // use commit in order to wait for disk-write before returning

        // save these preferences in case data directory is wiped
        copyFile(FILENAME_STANDARD_PATH, FILENAME_BACKUP_PATH, FILENAME_PREFS_FILE);


        // Post this so we don't try and copy the file until the file is written
        //Handler handler = new Handler();
        //handler.postDelayed(copyTask, 1000); // wait a second ?


        roamingMode = newRoamingMode;
        Log.d(TAG, "Restarting redbend app");
        Intent mStartActivity = new Intent(context, StartupActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
        // Runtime.getRuntime().exit(0);

    } // changeRoamingMode()

    //////////////////////////////////////////////////////////////////////
    // getRoamingMode()
    //  retrieve the current roaming MODE (Ignoring or Respecting
    //////////////////////////////////////////////////////////////////////
    public static int getRoamingMode(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(FILENAME_PREFS_KEY, context.MODE_PRIVATE);
        return prefs.getInt("roamingMode", MODE_RESPECT_ROAMING); // the current roaming mode to use

    } // getRoamingMode()





    static private Runnable copyTask = new Runnable() {
        @Override
        public void run() {
            try {

            } catch(Exception e) {
                Log.e(TAG,  "copyTask()", e);
            }
        }
    }; // copyTask()






    //////////////////////////////////////////////////////////////////////
    // File Operations
    //


    static boolean fileExists(String filePath, String fileName) {
        File f = new File(filePath, fileName);
        if (f.exists())
            return true;
        else
            return false;
    }


    //////////////////////////////////////////////////////////////////////
    // areFilePermissionsOK
    //  used to help determine if a file is safe to read or not
    // @return true if the file permissions are OK (owned by this user, not set to global-write-enable)
    //////////////////////////////////////////////////////////////////////
    static boolean areFilePermissionsOK(Context context, String filePath, String fileName) {
        // Checking File permissions directly was not introduced until API26 (Oreo), so we need to use su here.

        // First, figure out who we are


        PackageManager pm = context.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = pm.getApplicationInfo(context.getPackageName(), 0);
        } catch (Exception e) {
            Log.e(TAG, "Exception getting Application Info for " + context.getPackageName());
            return true; // assume we are OK
        }

        if (appInfo == null) {
            Log.e(TAG, "Application Info for " + context.getPackageName() + " is null!");
            return true; // assume we are OK
        }

        File file = new File(filePath, fileName);

        String command;
        command = "su -c 'ls -n " + file.getPath() + "'";
        Log.d(TAG, "Running " + command);

        String resultstr = "";

        try {
            java.lang.Process process = Runtime.getRuntime().exec(new String[] { "sh", "-c", command } );

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));


            resultstr = bufferedReader.readLine();


        } catch (Exception e) {
            Log.d(TAG, "Exception exec: " + command + ": " + e.getMessage());
            return true;
        }

        Log.d(TAG, "Returned: " + resultstr);

        if ((resultstr  == null) || (resultstr.isEmpty())) {
            Log.e(TAG, file.getPath() + " does not exist");
            return true; // no problem
        }

        String[] splits = resultstr.split("\\s+");

        if (splits.length < 3) {
            Log.e(TAG, command + " did not return expected results");
            return true; // unknown issue .. assume we are OK
        }
        // [0] contains permissions
        if ((!splits[0].endsWith("---"))) {
            Log.e(TAG, file.getPath() + " permissions must not allow global access. Ignoring file.");
            return false;
        }

        //[1,2] contains owner and group
        if (!splits[1].equals(splits[2])) {
            Log.e(TAG, file.getPath() + " owner \"" + splits[1] + "\" and group \"" + splits[2] + "\" are not the same. Ignoring file.");
            return false;
        }

        if (!splits[1].equals("" + appInfo.uid)) {
            Log.e(TAG, file.getPath() + " File owner is " + splits[1] + " , but expected " + appInfo.uid + " . Ignoring file.");
            return false;
        }


        return true; // OK

    } // areFilePermissionsOK()


    //////////////////////////////////////////////////////////////////////
    // copyFile()
    //  copies a file. This is used to store a backup of these shared Prefs in case the data directory is wiped.
    //////////////////////////////////////////////////////////////////////
    static boolean copyFile(String source_path, String destination_path, String filename) {
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        File src = null;
        File dst = null;

        boolean success = false;


        try {
            src = new File(source_path, filename);
            if (!src.exists()) return false; // the source file doesn't exist so we have nothing to do
        } catch (Exception e) {
            Log.e(TAG, "Unable to reference file " + source_path + "; " + e.toString());
            return false;
        }

        // OK, attempt the file copy

        Log.d(TAG, "Attempting copy " + filename + " from " + source_path + " to " + destination_path);

        try {
            // try to create directories if they don't already exist .. needed after uninstall
            File dstpath = new File(destination_path);
            dstpath.mkdirs();
            if (!dstpath.isDirectory()) {
                Log.e(TAG, "Unable to create destination directory " + destination_path);
                return false;
            }
        } catch(Exception e) {
            Log.e(TAG, "Unable to create directory " + destination_path + ";" + e.toString());
            return false;
        }

        try {


            dst = new File(destination_path, filename);
            inStream = new FileInputStream(src);
            outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
            success = true;
            Log.i(TAG, filename + " file was copied from " + source_path + " to " + destination_path);
        } catch (Exception e) {
            Log.w(TAG, filename  + "file could not be copied; " + e.toString());
        } finally {
            try {
                if (inStream != null)
                    inStream.close();
                if (outStream != null)
                    outStream.close();
            } catch (Exception e) {
                // Do Nothing
            }
        }

        return success;

    } // copyFile()



} // class MicronetRoaming

