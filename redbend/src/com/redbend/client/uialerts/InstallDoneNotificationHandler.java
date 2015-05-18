/*
 *******************************************************************************
 *
 * InstallDoneNotificationHandler.java
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;


import java.util.List;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.FlowManager;
import com.redbend.client.R;
import com.redbend.client.ClientService;

/**
 * Display an installation complete notification. The installation result is
 * defined in DMA_VAR_SCOMO_RESULT. 
 */
public class InstallDoneNotificationHandler extends NotificationHandlerBase {
	
	private static final int SCOMO_SUCCESS = 1200;
	private static final int SCOMO_PARTIAL_SUCCESS = 1452;
	private static final int FUMO_SUCCESS = 200;
	private static final String DMA_VAR_SCOMO_RESULT = "DMA_VAR_SCOMO_RESULT";
	private static final String DMA_VAR_SCOMO_DC_ID = "DMA_VAR_SCOMO_DC_ID";
	
	
	private int m_result;
	private Drawable m_appIcon = null;
	
	private static int[] m_singleAppActionResIdsArray = new int[]{
			R.string.successfully_installed_app, 	/*SCOMO_INSTALL_SUCCESS*/
			R.string.failed_install_app,         	/*SCOMO_INSTALL_SUCCESS_PARTIAL*/
			R.string.failed_install_app,			/*SCOMO_INSTALL_FAILED*/	
			R.string.successfully_removed_app,		/*SCOMO_REMOVE_SUCCESS*/
			R.string.failed_removed_app,			/*SCOMO_REMOVE_SUCCESS_PARTIAL*/
			R.string.failed_removed_app,			/*SCOMO_REMOVE_FAILED*/
			R.string.successfully_installed_app, 	/*FUMO_INSTALL_SUCCESS*/
			R.string.scomo_ins_err,					/*FUMO_INSTALL_FAILED*/
			R.string.successfully_updated_app, 		/*FUMO_AND_SCOMO_INSTALL_SUCCESS*/
			R.string.failed_update_app, 			/*FUMO_AND_SCOMO_INSTALL_PARTIAL*/
			R.string.failed_update_app, 			/*FUMO_AND_SCOMO_INSTALL_FAILED*/
			R.string.successfully_removed_app, 		/*FUMO_AND_SCOMO_REMOVE_SUCCESS*/
			R.string.failed_removed_app, 			/*FUMO_AND_SCOMO_REMOVE_PARTIAL*/
			R.string.failed_removed_app				/*FUMO_AND_SCOMO_REMOVE_FAILED*/
			};

	private static int[] m_appsListActionResIdsArray = new int[]{
			R.string.successfully_installed_x_apps, 	/*SCOMO_INSTALL_SUCCESS*/
			R.string.install_x_apps_partial_success,    /*SCOMO_INSTALL_SUCCESS_PARTIAL*/
			R.string.install_x_apps_failure,			/*SCOMO_INSTALL_FAILED*/	
			R.string.successfully_removed_x_apps,		/*SCOMO_REMOVE_SUCCESS*/
			R.string.remove_x_apps_partial_success,		/*SCOMO_REMOVE_SUCCESS_PARTIAL*/
			R.string.remove_x_apps_failure,				/*SCOMO_REMOVE_FAILED*/
			R.string.fumo_update,						/*FUMO_INSTALL_SUCCESS*/
			R.string.fumo_update,						/*FUMO_INSTALL_FAILED*/
			R.string.successfully_installed_x_updates,	/*FUMO_AND_SCOMO_INSTALL_SUCCESS*/
			R.string.install_x_updates_partial_success, /*FUMO_AND_SCOMO_INSTALL_PARTIAL*/
			R.string.install_x_updates_failure,			/*FUMO_AND_SCOMO_INSTALL_FAILED*/
			R.string.successfully_removed_x_updates,	/*FUMO_AND_SCOMO_REMOVE_SUCCESS*/
			R.string.remove_x_updates_partial_success,	/*FUMO_AND_SCOMO_REMOVE_PARTIAL*/
			R.string.remove_x_updates_failure			/*FUMO_AND_SCOMO_REMOVE_FAILED*/
			};

	private static final int[] m_contentTextResIdsArray = new int[]{
			R.string.successfully_installed,			/*SCOMO_INSTALL_SUCCESS*/
			R.string.update_partial_success,			/*SCOMO_INSTALL_SUCCESS_PARTIAL*/
			R.string.update_failure, 					/*SCOMO_INSTALL_FAILED*/
			R.string.successfully_removed,				/*SCOMO_REMOVE_SUCCESS*/
			R.string.remove_partial_success, 			/*SCOMO_REMOVE_SUCCESS_PARTIAL*/
			R.string.remove_failure,					/*SCOMO_REMOVE_FAILED*/
			R.string.successfully_installed, 			/*FUMO_INSTALL_SUCCESS*/
			R.string.update_failure,					/*FUMO_INSTALL_FAILED*/
			R.string.scomo_update_done, 				/*FUMO_AND_SCOMO_INSTALL_SUCCESS*/
			R.string.update_partial_success,			/*FUMO_AND_SCOMO_INSTALL_PARTIAL*/
			R.string.update_failure, 					/*FUMO_AND_SCOMO_INSTALL_FAILED*/
			R.string.successfully_removed,				/*FUMO_AND_SCOMO_REMOVE_SUCCESS*/
			R.string.remove_partial_success, 			/*FUMO_AND_SCOMO_REMOVE_PARTIAL*/
			R.string.remove_failure 					/*FUMO_AND_SCOMO_REMOVE_FAILED*/
			};
	
	
	public InstallDoneNotificationHandler(Context ctx){
		super(ctx);
	}
	
	private Drawable getApplicationIcon(Event ev) {
		byte[] t = ev.getVarStrValue(DMA_VAR_SCOMO_DC_ID);
		if (t == null) {
			return null;
		}
		String packageName = new String(t);
		Log.d(LOG_TAG, "getApplicationIcon=>inPackageName: " + packageName);

		Drawable appIcon = null;
		PackageManager pm = ctx.getPackageManager();
		List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
		for (PackageInfo p : packageInfoList) {
			Log.d(LOG_TAG, "getApplicationIcon=>p.packageName: "
					+ p.packageName);
			if (packageName.compareToIgnoreCase(p.packageName) == 0) {
				appIcon = p.applicationInfo.loadIcon(pm);
				break;
			}
		}
		return appIcon;
	}
	
	private void IconsResourcesInit(Event ev) {
		// icons
		if (m_isInstall){
			// get application icon, if list len == 1
			if (m_result == SCOMO_SUCCESS || m_result == FUMO_SUCCESS) {
				if (m_moType == MoType.SCOMO && m_appListLen == 1) {
						m_appIcon = getApplicationIcon(ev);
					}
				m_largeIconResId = R.drawable.success_icon;
				m_smallIconResId = R.drawable.dlandins_complete;
			} else { // failed to install
				m_largeIconResId = R.drawable.failure;
				m_smallIconResId = R.drawable.failure_small;
			}
		} else {// remove
			m_largeIconResId = R.drawable.remove_app;
			m_smallIconResId = R.drawable.remove_small;
		}
	}
	
	private int getCorrectIndex() {
		boolean[] conditionsArray = new boolean[] {
				(m_moType == MoType.SCOMO && m_isInstall && m_result == SCOMO_SUCCESS),
				(m_moType == MoType.SCOMO && m_isInstall && m_result == SCOMO_PARTIAL_SUCCESS),
				(m_moType == MoType.SCOMO && m_isInstall && (m_result != SCOMO_SUCCESS && m_result != SCOMO_PARTIAL_SUCCESS)),
				(m_moType == MoType.SCOMO && !m_isInstall && m_result == SCOMO_SUCCESS),
				(m_moType == MoType.SCOMO && !m_isInstall && m_result == SCOMO_PARTIAL_SUCCESS),
				(m_moType == MoType.SCOMO && !m_isInstall && (m_result != SCOMO_SUCCESS && m_result != SCOMO_PARTIAL_SUCCESS)),
				(m_moType == MoType.FUMO && m_isInstall && m_result == FUMO_SUCCESS),
				(m_moType == MoType.FUMO && m_isInstall && m_result != FUMO_SUCCESS),
				(m_moType == MoType.FUMO_IN_SCOMO && m_isInstall && m_result == SCOMO_SUCCESS),
				(m_moType == MoType.FUMO_IN_SCOMO && m_isInstall && m_result == SCOMO_PARTIAL_SUCCESS),
				(m_moType == MoType.FUMO_IN_SCOMO && m_isInstall && (m_result != SCOMO_PARTIAL_SUCCESS && m_result != SCOMO_SUCCESS)),
				(m_moType == MoType.FUMO_IN_SCOMO && !m_isInstall && m_result == SCOMO_SUCCESS),
				(m_moType == MoType.FUMO_IN_SCOMO && !m_isInstall && m_result == SCOMO_PARTIAL_SUCCESS),
				(m_moType == MoType.FUMO_IN_SCOMO && !m_isInstall && (m_result != SCOMO_PARTIAL_SUCCESS && m_result != SCOMO_SUCCESS)) };

		for (int i = 0; i < conditionsArray.length ; i++) {
			if (conditionsArray[i]) {
				return i;
			}
		}
		return ERROR;		
	}
	

	@Override
	protected int initiate(Event ev) {
		m_eventName = ev.getName();		
		m_appIcon = null;
		
		m_moType = getMoType(ev.getVarValue("DMA_VAR_OPERATION_TYPE"));
		m_result = ev.getVarValue(DMA_VAR_SCOMO_RESULT);

		prepareApplicationNamesString(ev);
		if (m_appListLen == 0) {
			return ERROR;
		}
		
		Log.d(LOG_TAG, "initiate=>m_isInstall = " + m_isInstall + " m_moType = " + m_moType + " m_Result = " + m_result);
		int index = getCorrectIndex();
		if(index == ERROR){
			return ERROR;
		}

		m_resIdSingleApp = m_singleAppActionResIdsArray[index];
		m_resIdAppsList = m_appsListActionResIdsArray[index];
		m_resIdContentText = m_contentTextResIdsArray[index];

		IconsResourcesInit(ev);
		return SUCCESS;
	}
	
	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
		throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");	
		((ClientService)ctx).startFlowInBackground(flowId);
		
		if(initiate(ev) == ERROR){
			Log.d(LOG_TAG, "-notificationHandler=>initiate notification problems, return null");	
			return null;
		}

		if(m_notificationBuilder == null){
			m_notificationBuilder = new Notification.Builder(ctx);
		}
		
		m_notificationBuilder.setSmallIcon(m_smallIconResId)
			.setContentIntent(FlowManager.getReturnToFgIntent(ctx, flowId));
		
		if(m_appIcon != null){
			Bitmap bitmap = ((BitmapDrawable)m_appIcon).getBitmap();
			m_notificationBuilder.setLargeIcon(bitmap);
		}else{
			m_notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), m_largeIconResId));
		}
		
		m_notificationBuilder.setContentTitle(getContentTitle());
		m_notificationBuilder.setContentText(getContentText());
		m_notificationBuilder.setTicker(getTickerText());
		
		Log.d(LOG_TAG, "-notificationHandler");
		return m_notificationBuilder;
	}
}
