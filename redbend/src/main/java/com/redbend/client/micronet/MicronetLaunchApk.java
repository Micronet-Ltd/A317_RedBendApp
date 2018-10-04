
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.StringTokenizer;


public class MicronetLaunchApk {

    public static final String TAG = "RBC-LaunchApk";





	public static boolean checkIsLaunchComponent(String componentId) {

		if (componentId.startsWith(".")) {
			Log.i(TAG, "Component ID " + componentId + " indicates this is a launch-apk-after-install operation");
			return true;
		}
		return false;

	}


	public static String getRealPackageName(String componentId) {

		// everything except the first character (first character is a ^)

		String realName = componentId;

		if (componentId.startsWith(".")) // this means auto-launch
			realName = componentId.substring(1); // skip the period to get the package name

		Log.i(TAG, "Extracted package name is " + realName);

		return realName;

	}




	/////////////////////////////////////////////////////////////////////////////////
	// installOrRemoveApk()
	//   Tries to install or (remove) the package
	//		This will also try to launch the package after install
	//  mode: 1 = Install, 2 = Update, 3 = Delete
	//  dcPath: the temporary location of the apk file
	//  returns
	/////////////////////////////////////////////////////////////////////////////////
	public static int installOrRemoveApk(Context context, String componentId, int mode,
										 String sourcePath, long source_offset, long source_length) {

		/* values taken from swm_general_errors.h */
		final int SWM_UA_ERR_FAILED_TO_UNINSTALL_APK = 0x0200;
		final int SWM_UA_ERR_FAILED_TO_INSTALL_APK = 0x0201;


		int ret = 0; // initial value, will get overriden
		switch (mode) {
			case 1: // install
			case 2: // upgrade

				// create a spot to store a temporary APK file
				int index = sourcePath.lastIndexOf("/");
				String temporaryPath = sourcePath.substring(0, index) + "/" + componentId;

				// copy data to the temporary APK file
				if (!MicronetFileUpload.copyFileFromRange(sourcePath, temporaryPath , source_offset, source_length)) {
					ret = SWM_UA_ERR_FAILED_TO_INSTALL_APK;
				}
				// install from the temporary APK file
				if (ret == 0) {
					ret = runProcess("pm", "install", "-r", temporaryPath) ? 0 : SWM_UA_ERR_FAILED_TO_INSTALL_APK;
				}

				// remove the temporary APK file
				MicronetFileUpload.removeFile(temporaryPath);

				// should we launch this package after install ?

				if (ret == 0) {
					String realPackageName = getRealPackageName(componentId);
					ret = (launchFromPackageName(context, realPackageName)  ? 0 : SWM_UA_ERR_FAILED_TO_INSTALL_APK);
				}

				break;

			case 3: // uninstall
				// remove the package
				String realPackageName = getRealPackageName(componentId);
				ret = runProcess("pm", "uninstall", realPackageName) ? 0 : SWM_UA_ERR_FAILED_TO_UNINSTALL_APK;
				break;
			default: // should never reach ehre
				Log.e(TAG, "Unknown value for mode (= " + mode + ")");
				ret = SWM_UA_ERR_FAILED_TO_UNINSTALL_APK;
				break;


		} // switch

		Log.d(TAG, "installOrRemoveApk = " + ret);
		return ret; // OK

	}





	/////////////////////////////////////////////////////////////////////////////////
    //	launchFromApkFile()
    //  Looks up the package inside the given apk and launches the launch component/action    
    // Returns:
    //      true if something was launched, false if it was not.
    /////////////////////////////////////////////////////////////////////////////////
    public static boolean launchFromApkFile(Context context, String launchFileName ) {
		Log.i(TAG, "Launching from APK: " + launchFileName);


		String packageName= null;

		try {

			// Try and get the name of the package from the apk:
			PackageManager pm = context.getPackageManager();
			PackageInfo packageInfo = pm.getPackageArchiveInfo(launchFileName, 0);


			if (pm == null) {
				Log.e(TAG, "Could not retrieve package name from apk path " + launchFileName);
				return false; // make sure an error makes it back to the website
			}

			packageName = packageInfo.packageName;

			if (packageName == null) {
				Log.e(TAG, "Retrieved a null package name from apk path " + launchFileName);
				return false; // make sure an error makes it back to the website
			}

			Log.i(TAG, "Retrieved Package Name: " + packageInfo.packageName);

		} catch (Exception e) {
			Log.e(TAG, "Exception getting package name from apk: " + e.getMessage());
			return false; // cannot launch
		}

		return launchFromPackageName(context, packageName);

	}


	/////////////////////////////////////////////////////////////////////////////////
	//	launchFromPackageName()
	//  Tries to launch the component/action given the package name
	// Returns:
	//      true if something was launched, false if it was not.
	/////////////////////////////////////////////////////////////////////////////////
	public static boolean launchFromPackageName(Context context, String startingPackageName ) {

		Log.i(TAG, "Launching from Package: " + startingPackageName );



			// Try and get the main/launch activity for the package
		try {
			PackageManager pm = context.getPackageManager();
			Intent intent=pm.getLaunchIntentForPackage(startingPackageName );
					
			String intentPackageName = intent.getPackage();
			ComponentName component = intent.getComponent();
			String componentName = "";
			if (component != null) {
				componentName =  component.getClassName();
			}
			String actionName = intent.getAction();
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this makes sure it starts if it isn't running.
			
			// Launch the Activity
			Log.i(TAG, "Starting Intent: package " + intentPackageName  + " ; component " + componentName + " ; action " + actionName);
			context.startActivity(intent);

			//Log.i (TAG, "Done with Launch");

		} catch (Exception e) {
			Log.e(TAG, "Exception Launching File: " + e.getMessage());
			return false;
		}
		return true;
    } // launchFromPackageName()


	private static boolean runProcess(String... cmd) {
		boolean ret = true;
		StringBuilder command = new StringBuilder();

		for (String c : cmd) {
			command.append(' ');
			command.append(c);
		}
		command.deleteCharAt(0);

		try {
			Process process = new ProcessBuilder().command(cmd)
					.redirectErrorStream(true).start();

			Reader reader = new InputStreamReader(process.getInputStream());
			int chr;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((chr = reader.read()) != -1)
				out.write(chr);
			String str = out.toString("UTF-8");
			out.close();
			reader.close();
			Log.d(TAG, "runProcess buffer: " + str);
			ret = str.contains("Failure") ? false : true;

			Log.i(TAG, "Finished executing '" + command + "', ret=" + ret);
			process.destroy();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, Log.getStackTraceString(e));
		}
		return ret;
	}


} // class MicronetLaunchApk
