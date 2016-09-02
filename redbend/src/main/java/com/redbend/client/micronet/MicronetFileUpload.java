
/*******************************************************************************
//
// MicronetFileUpload
//
// Functionality to allow uploading a file to a specific location (instead of installing an APK)


 // Changes Required to Client:
 //     call these functions from InstallApk.java


******************************************************************************/

/**
 * Created by dschmidt on 8/5/16.
 */

package com.redbend.client.micronet;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;


public class MicronetFileUpload {

    public static final String LOG_TAG = "RBC-FileUpload";


    /////////////////////////////////////////////////////////////////////////////////
    // checkIsCopyFile()
    //  checks if the operation is a copy-file operation (instead of an install apk operation)
    //      true if this should be treated like a file-copy operation, false if it should not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean checkIsCopyFile( String apkFileName ) {

        if (apkFileName.contains("File_")) {
            // if the apk file name starts with "File_" (not counting path), then this is not an apk. Instead, the file should be copied to specified location.
            return true;
        }
        return false;
    }


    /////////////////////////////////////////////////////////////////////////////////
    // copyFile()
    //  copies the given file to the specified location (used instead of installing an apk)
    //  called from InstallApk class if the "installation" is really a file copy
    // Returns:
    //      true if file was copied, false if it was not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean copyFile( String copyFileName ) {
        Log.i (LOG_TAG, "Copying File instead of installing: " + copyFileName);
        String FileName = "";
        String DirName = "/";

        InputStream in = null;
        OutputStream out = null;
        File dir = null;

        //String prefix = "File_";
        String prefix = "/data/data/com.redbend.client/files/temp_folder/File_";
        String noPrefixStr = copyFileName.substring(copyFileName.indexOf(prefix) + prefix.length());
        Log.i (LOG_TAG, "The noPrefixStr: " + noPrefixStr);

        boolean result = false;

        //String[] tokens = noPrefixStr.split(";");

        //for (String t : tokens)
        //	DirName = DirName + "/" + t;

        try {
            StringTokenizer tokens = new StringTokenizer( noPrefixStr, ";" );
            if (null != tokens){
                int elementNum = tokens.countTokens();
                Log.i (LOG_TAG, "The found elementsNum: " + elementNum);
                if ( 2 >= elementNum ) {
                    Log.e (LOG_TAG, "The Configuration name wrong !!!!!" + copyFileName);
                    return false;
                }

                for (int i = 0; i < elementNum; i++)
                {
                    if ( i < (elementNum - 1))
                        DirName = DirName + "/" + tokens.nextToken();
                    else {
                        FileName = tokens.nextToken();
                        //DirName = DirName + "/";
                    }
                }

                Log.i (LOG_TAG, "The found file name  " + FileName);
                FileName = FileName.replace(".apk", "");
                Log.i (LOG_TAG, "The found file name correction  " + FileName);
                Log.i (LOG_TAG, "The found Dir name  " + DirName);

                //Start copy procedure

                //Check if directory exist
                //and if not create new
                dir = new File(DirName);
                if (null != dir){
                    if (!dir.exists())
                    {
                        dir.mkdirs();
                    }
                } else {
                    Log.e (LOG_TAG, "Error open or create folder !!!!!" + DirName);
                    return false;
                }

                in = new FileInputStream(copyFileName);
                out = new FileOutputStream(DirName + "/" + FileName);

                Log.i (LOG_TAG, "Start Copy");
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                //finish copy
                out.flush();

                Log.i (LOG_TAG, "Finish Copy");

                //delete file from update directory
                //new File(copyFileName).delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != in)
                    in.close();
                result = true; // 2016-08-05 DS -- not sure why this is here and not up near Finish Copy ??
            }catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (null != out)
                    out.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }


        return result;
    } // copyFile()




} // class MicronetFileUpload
