
/*******************************************************************************
 //
 // MicronetDevice
 //
 // Functionality to get information about indentifying the device
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
