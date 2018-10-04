
/*******************************************************************************
 //
 // MicronetFileReboot
 //
 // Functionality to control the rebooting of the device
 //     this gets called in addition to the built-in reboot mechanism in the client


 // Changes Required to Client:
 //     call these functions from /ui/RequestReboot.java


 ******************************************************************************/



package com.redbend.client.micronet;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dschmidt on 8/24/16.
 */
public class MicronetReboot {

    public static final String LOG_TAG = "RBC-Reboot";

    ///////////////////////////////////////////////////////
    // prepareRebootFlags()
    //  prepares the recovery flags, etc. in anticipation of a reboot
    ///////////////////////////////////////////////////////
    static public void prepareRebootFlags() {


        String fnrk = "/runningkernel";
        String fncf = "/data/misc/rb/continueflag";
        String fnrf = "/data/misc/rb/recoveryflag";


        //check that running kernel primary
        File f = new File(fncf);
        if (f.exists()) {
            File cfc = new File(fncf);
            if (cfc.exists()) {
                File frk = new File(fnrk);
                if (frk.exists()) {
                    String Runningkernel;
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(fnrk), 256);
                        try {
                            Runningkernel = reader.readLine();
                        } finally {
                            reader.close();
                        }
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Exception reading running kernel file");
                        return;
                    }

                    if (Runningkernel.equals("primary") != true) {
                        Log.e(LOG_TAG, "Error: running [" + Runningkernel + "] or secondary kernel although continue flag exists");
                        return; //If continue flag exists primary kernel must be the one loaded
                    }
                } else {
                    Log.e(LOG_TAG, "Error: no running kernel file");
                    return;//No way of knowing which kenrel is running
                }
            }
        } else {
            Log.d(LOG_TAG, "no continue flag - which running kernel not checked");
        }
        //About to reset and run recovery. Create the recovery flag.
        File file = new File(fnrf);

        try {
            file.createNewFile();
        } catch(Exception e) {
            Log.e(LOG_TAG, "Error: failed to create recovery flag");
            return;
        }
        if(file.exists()) {
            try {
                OutputStream fo = new FileOutputStream(file);
                fo.write(1);
                fo.close();
                Log.d(LOG_TAG, "created recovery flag");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error: failed to create output stream for recovery flag");
                return;
            }
        } else {
            Log.e(LOG_TAG, "Error: failed to create recovery flag - file doesn't exist");
            return;
        }

        //remove continue flag before reset
        File fncfins = new File(fncf);
        if (fncfins.exists()) {
            fncfins.delete();
        }


    } // prepareRebootFlags()


} // class MicronetReboot
