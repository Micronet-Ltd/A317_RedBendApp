
/*******************************************************************************
 //
 // MicronetDevice
 //
 // Functionality to identify the device (get the Device ID) that is used in communication with the server


 // Changes Required to Client:
 //     call this function from Ipl.java file

 ******************************************************************************/
/**
 * Created by dschmidt on 8/5/16.
 */

package com.redbend.client.micronet;



import android.os.Build;
import android.util.Log;

public class MicronetDevice {

    public static final String LOG_TAG = "RBC-Device";

    /////////////////////////////////////////////////
    // getDeviceId()
    //      the device ID is always the serial number of the device.
    /////////////////////////////////////////////////
    public static String getDeviceId() {
        String deviceId;

        deviceId=  Build.SERIAL;

        Log.d(LOG_TAG,"DeviceId:" + deviceId);
        return deviceId;

    }

} // class MicronetDevice
