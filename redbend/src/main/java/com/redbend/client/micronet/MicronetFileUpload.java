
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

import com.redbend.ComponentInfo;
import com.redbend.client.GenericInstallerHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;


public class MicronetFileUpload {

    public static final String LOG_TAG = "RBC-FileUpload";



    /////////////////////////////////////////////////////////////////////////////////
    // These methods deal with the old-style APK Installation
    //  In this method the file is encapsulated into an APK before uploading to the server
    /////////////////////////////////////////////////////////////////////////////////

    // If the name starts with one of these, then this is a command to copy the file:
    public static final String APK_NAME_PREFIX = "/data/data/com.redbend.client/files/temp_folder/File_"; // OLD convention


    /////////////////////////////////////////////////////////////////////////////////
    // checkIsApkCopyFile()
    //  checks if the operation is a copy-file operation (instead of an install apk operation)
    //      true if this should be treated like a file-copy operation, false if it should not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean checkIsApkCopyFile( String apkFileName ) {

        if (apkFileName.contains("File_")) {
            // if the apk file name starts with "File_" (not counting path), then this is not an apk. Instead, the file should be copied to specified location.
            return true;
        }
        return false;
    }


    /////////////////////////////////////////////////////////////////////////////////
    // convertApkCopyFileName()
    //  extracts the real file name (what the placed file will be called) from the name used inside the apk file
    // Parameters:
    //  redbendFileName: format = "/data/data/com.redbend.client/files/temp_folder/File_" PATH_WITH_SEMICOLONS + "[.apk]"
    //      e.g. "/data/data/com.redbend.client/files/temp_folder/File_data;internal_Storage;testupload.txt.apk";
    //          Note: this will translate to "/data/internal_Storage/testupload.txt"
    // Returns:
    //      null if it could not be extracted, otherwise
    //      the true file name (e.g. "/data/internal_Storage/testupload.txt")
    /////////////////////////////////////////////////////////////////////////////////
    static String convertApkCopyFileName(String redbendFileName) {


        Log.i(LOG_TAG, "Extracting true file name from " + redbendFileName);

        //String prefix = "File_";
        final String prefix = APK_NAME_PREFIX; // "/data/data/com.redbend.client/files/temp_folder/File_";
        String noPrefixStr = redbendFileName.substring(redbendFileName.indexOf(prefix) + prefix.length());
        String fileName = noPrefixStr.replace(".apk", "");





        // make sure file always starts with the delimiter (will become the backslash to make the path absolute)
        if (fileName.charAt(0) != ';')
            fileName = ";" + fileName;

        // Safeties to make sure you don't overwrite key files
        // (The only safety existing in original implementation was that it cant be in root directory

        String[] splits = fileName.split(";");

        if (splits.length < 3) {
            Log.e (LOG_TAG, "File must not be in root directory: " + fileName);
            return null;
        } if ((splits[1].length() == 0) || (splits[2].length() == 0)) {
            Log.e (LOG_TAG, "Directory and/or File names must not be empty: " + fileName);
            return null;
        }

        // convert the semicolons to slashes to get our actual name
        fileName.replace(';', '/');

        // make sure we always start with backslash to create are an absolute path

        Log.i (LOG_TAG, "Extracted " + fileName);

        return fileName;

    } // convertApkFileName()



    /////////////////////////////////////////////////////////////////////////////////
    // copyApkFile()
    //  Extracts the name from the path and then
    //  copies the given file to the specified location (used instead of installing an apk)
    //  called from InstallApk class if the "installation" is really a file copy
    // Returns:
    //      true if file was copied, false if it was not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean copyApkFile( String redbendFileName ) {


        String fileName = convertApkCopyFileName(redbendFileName);

        if (fileName == null) {
            return false; // unable to copy the file
        }


        return copyFile(redbendFileName, fileName);

    }


    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    // These methods deal with the new-style File Installation
    //  In this method the file does not need to be encapsulated before uploading to the server
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////




    public static boolean checkIsCopyComponent(String componentId) {

        // Note: with slashes or backslashes, the file will get installed, but the tree.xml operations will jam
        // Thus a user should not ever give the software an ID with a slashes or backslashes -- always use semicolons instead.

        if ((componentId.startsWith("\\")) ||
                (componentId.startsWith("/")) ||
                (componentId.startsWith(";"))) {
            Log.i(LOG_TAG, "Component ID " + componentId + " indicates this is a copy-file operation");
            return true;
        }
        return false;

    }




    /////////////////////////////////////////////////////////////////////////////////
    // getRealFileName()
    //  gets the final file name from the componentId
    // Replace any semicolons and backslashes with forward slashes to get a true file&path name
    /////////////////////////////////////////////////////////////////////////////////
    public static String getRealFileName(String componentId) {
        // replace all semicolons or backslashes with slashes
        String realFileName = componentId.replace(';', '/');
        realFileName = realFileName.replace('\\', '/');

        // eliminate the '#' sign and anything after it (needed to avoid duplicate IDs)
        int lastNumberSignIndex =  realFileName.lastIndexOf("#");
        if (lastNumberSignIndex > 0)
            realFileName = realFileName.substring(0, lastNumberSignIndex);

        Log.i(LOG_TAG, "Extracted File name is " + realFileName);
        return realFileName;
    }


    /////////////////////////////////////////////////////////////////////////////////
    // installOrRemoveFile()
    //   Tries to install or (remove) the File to a desired directory
    //  mode: 1 = Install, 2 = Update, 3 = Delete
    //  dcPath: the temporary location of the file
    //  returns
    /////////////////////////////////////////////////////////////////////////////////
    public static int installOrRemoveFile(String componentId, int mode, String sourcePath, long source_offset, long source_length) {

		/* values taken from swm_general_errors.h */
        final int SWM_UA_ERR_FAILED_TO_UNINSTALL_APK = 0x0200;
        final int SWM_UA_ERR_FAILED_TO_INSTALL_APK = 0x0201;


        // Convert the component ID to the file name
        String destinationPath = MicronetFileUpload.getRealFileName(componentId);

        switch (mode) {
            case 1: // install
            case 2: // upgrade
                // copy the file to the destination
                if (!MicronetFileUpload.copyFileFromRange(sourcePath, destinationPath, source_offset, source_length)) {
                    return SWM_UA_ERR_FAILED_TO_INSTALL_APK;
                }
                return 0; // OK

            case 3: // uninstall
                // remove the file at the destination
                if (MicronetFileUpload.removeFile(destinationPath)) return SWM_UA_ERR_FAILED_TO_UNINSTALL_APK;
                return 0;

        } // switch

        // Should never reach here (unknown mode)
        return SWM_UA_ERR_FAILED_TO_UNINSTALL_APK;
    }


    /////////////////////////////////////////////////////////////////////////////////
    // getComponentInfo()
    //  This is called to check whether the file is installed and get the the "version" of the file
    //  returns the version of the file if it is installed (always = 1 for now)
    /////////////////////////////////////////////////////////////////////////////////
    public static ComponentInfo getComponentInfo(String componentId) {


        String realFileName = getRealFileName(componentId);
        if (!doesFileExist(realFileName)) {
            Log.d(LOG_TAG, "getComponentInfo(): File " + realFileName + " may not exist, returning null");
            return null;
        }

        // File exists, return that it is present at version 1

        Log.d(LOG_TAG, "getComponentInfo(): File " + realFileName + " exists, returning version=1");

        ComponentInfo ci = new ComponentInfo();
        ci.description = "";
        ci.id = componentId;
        ci.installedPathName = componentId; // the ID is the name of the file
        ci.name = componentId; // its also the name
        ci.partNumber = "";
        ci.version = "1";

        return ci;
    }



    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    // Generic File Operations (could be used for both methods)
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////




    /////////////////////////////////////////////////////////////////////////////////
    // removeFile()
    //  removes the given file to the specified location
    // Returns:
    //      true if file was removed, false if it was not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean removeFile( String fileName ) {

        Log.i(LOG_TAG, "Removing File " + fileName);

        if (fileName == null) {
            return false; // unable to remove the file
        }

        try {
            if (fileName != null)
            {
                File file = new File(fileName);
                if (file.exists())
                    file.delete();
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception trying to remove file " + fileName + e.getMessage());
            e.printStackTrace();
        }

        return true;
    } // removeFile()


    static boolean doesFileExist(String fileName) {
        try {
            File file = new File(fileName);
            return file.exists();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error checking if fileexists: " + fileName);
            return false;
        }

    }

    private static boolean createDirectoryIfNeeded(String destinationFileName) {
        // Check if directory exists and create it if it does not.

        int last_separator = destinationFileName.lastIndexOf("/");

        if (last_separator < 0) {
            Log.e(LOG_TAG, "Cannot extract directory from file name: " + destinationFileName);
            return false;
        }

        String directoryName = destinationFileName.substring(0, last_separator);

        try {
            File dir = new File(directoryName);
            if (!dir.exists()) {
                Log.i(LOG_TAG, "Creating directory " + directoryName);
                dir.mkdirs();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error opening or creating directory: " + directoryName);
            return false;
        }

        return true;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // copyFile()
    //  copies the given file to the specified location
    // Returns:
    //      true if file was copied, false if it was not.
    /////////////////////////////////////////////////////////////////////////////////
    static boolean copyFile(String sourceFileName, String destinationFileName) {

        // Log.i(LOG_TAG, "Copying File to " + destinationFileName);

        long fileLength = 0;
        try {
            File file = new File(sourceFileName);
            fileLength = file.length();
        } catch (Exception e) {
            Log.e(LOG_TAG, "copyFile: Cannot get length of source: " + sourceFileName);
        }

        if (fileLength > 0) {
            return copyFileFromRange(sourceFileName, destinationFileName, 0, -1);
        } else {
            return false;
        }

    } // copyFile()



    public static boolean copyFileFromRange(String sourceFileName, String destinationFileName, long source_offset, long bytesToCopy) {
        int readBytes = 0;
        int bufferSize = 8192;
        byte[] buf = new byte[bufferSize];


        Log.d(LOG_TAG, "Copying to " + destinationFileName + " offset=" + source_offset + " length= " + bytesToCopy
                + " bufferSize=" + bufferSize);


        createDirectoryIfNeeded(destinationFileName);

        try {
            RandomAccessFile fin = new RandomAccessFile(sourceFileName, "r");
            RandomAccessFile fout = new RandomAccessFile(destinationFileName, "rw");
            fin.seek(source_offset);

            //Start copy component from DP into the target file
            while (bytesToCopy > 0) {
                readBytes = fin.read(buf);
                if (readBytes != -1) {

                    // make sure we don't write more bytes than we were asked to.
                    if (readBytes > bytesToCopy) readBytes = (int) bytesToCopy;

                    fout.write(buf,0,readBytes) ;
                    bytesToCopy -= readBytes;
                } else
                    Log.e(LOG_TAG, "unexpected end of file");
            }
            fin.close();
            fout.close();
            File file = new File(destinationFileName);
            file.setReadable(true, false);
            //file.setExecutable(true, false);

        } catch (IOException ioE) {
            Log.d(LOG_TAG, Log.getStackTraceString(ioE));
            return false;
        }

        Log.d(LOG_TAG, "Finished copying");
        return true;
    }


} // class MicronetFileUpload
