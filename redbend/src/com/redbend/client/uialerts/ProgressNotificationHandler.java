/*
 *******************************************************************************
 *
 * ProgressNotificationHandler.java
 *
 * Displays a download progress notification, or handles a download failure or
 * cancelation. The current progress value is defined in DMA_VAR_DL_PROGRESS or
 * DMA_VAR_INSTALL_PROGRESS.
 * 
 * Receives DIL events:
 * DMA_MSG_SCOMO_DL_PROGRESS (not silent update only, background only)
 * DMA_MSG_SCOMO_INSTALL_PROGRESS (not silent update only, background only)
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.client.uialerts;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import com.redbend.app.Event;
import com.redbend.app.FlowManager;
import com.redbend.client.R;

/**
 * Display progress notification
 */
public class ProgressNotificationHandler extends NotificationHandlerBase {
	
	private static final int MAX_PROGRESS = 100;	
	private static final String DMA_MSG_SCOMO_INSTALL_PROGRESS_UI = "DMA_MSG_SCOMO_INSTALL_PROGRESS_UI";
//	private static final String DMA_VAR_SCOMO_INSTALL_PROGRESS = "DMA_VAR_SCOMO_INSTALL_PROGRESS";
	private static final String DMA_VAR_DL_PROGRESS = "DMA_VAR_DL_PROGRESS";

	private String m_contentText = null;
	private String m_contentTitle = null;
	private String m_tickerText = null;
	private String m_downloadingPercentage = null;
	private int m_progressPercentage;
	
	private static int[] m_appsListActionResIdsArray = new int[]{
		R.string.installing_x_apps,                     /*SCOMO_INSTALL*/
		R.string.removing_x_apps,						/*SCOMO_REMOVE*/
		R.string.downloading_x_apps,					/*SCOMO_DOWNLOAD*/
		R.string.fumo_update,							/*FUMO_INSTALL*/
		R.string.fumo_update,							/*FUMO_DOWNLOAD*/
		R.string.installing_x_updates,					/*FUMO_AND_SCOMO_INSTALL*/
		R.string.downloading_x_updates};				/*FUMO_AND_SCOMO_DOWNLOAD*/
	
	

	public ProgressNotificationHandler(Context ctx) {
		super(ctx);
	}
	
	private int getCorrectIndex(boolean isDownloadingEvent) {
		 boolean conditionsArray[] = new boolean[] {
				(m_moType == MoType.SCOMO && m_isInstall && !isDownloadingEvent), 		/*SCOMO_INSTALL*/
				(m_moType == MoType.SCOMO && !m_isInstall && !isDownloadingEvent),		/*SCOMO_REMOVE*/
				(m_moType == MoType.SCOMO && isDownloadingEvent),							/*SCOMO_DOWNLOAD*/
				(m_moType == MoType.FUMO && !isDownloadingEvent),							/*FUMO_INSTALL*/
				(m_moType == MoType.FUMO && isDownloadingEvent),							/*FUMO_DOWNLOAD*/
				(m_moType == MoType.FUMO_IN_SCOMO && m_isInstall && !isDownloadingEvent),	/*FUMO_AND_SCOMO_INSTALL*/
				(m_moType == MoType.FUMO_IN_SCOMO && isDownloadingEvent)					/*FUMO_AND_SCOMO_DOWNLOAD*/
			};
			
			for (int i = 0; i < conditionsArray.length; i++) {
				if(conditionsArray[i]){
					return i;
				}
			}
			return ERROR;
	}

	@Override
	protected int initiate(Event ev) {
		m_eventName = ev.getName();
		boolean isDownloadingEvent = true;
		m_moType = getMoType(ev.getVarValue("DMA_VAR_OPERATION_TYPE"));
		m_downloadingPercentage = null;		
		
		prepareApplicationNamesString(ev);		
		if (m_appListLen == 0) {
			return ERROR;
		}
		
		if (m_eventName.equals(DMA_MSG_SCOMO_INSTALL_PROGRESS_UI)) {
			// installation event
			isDownloadingEvent = false;
			if(!m_isInstall){ //removing
				m_smallIconResId = R.drawable.uninstall_anim;
				m_resIdSingleApp = R.string.scomo_remove_progress;
				m_resIdContentText = R.string.removing;				
			}else{	//installing
				m_smallIconResId = android.R.drawable.stat_sys_download;
				m_resIdSingleApp = R.string.scomo_install_progress;
				m_resIdContentText = R.string.installing;
//				progressStatusVarName = DMA_VAR_SCOMO_INSTALL_PROGRESS;				
			}
		}else{
			m_resIdSingleApp = R.string.scomo_downloading_notif_title;
			m_resIdContentText = R.string.downloading;
				try {
					m_progressPercentage = ev.getVar(DMA_VAR_DL_PROGRESS)
							.getValue();
				} catch (Exception e) {
					Log.e(LOG_TAG, "Wrong event " + m_eventName + " value "
							+ DMA_VAR_DL_PROGRESS);
					return ERROR;
			}
			m_downloadingPercentage = String.format(
					ctx.getString(R.string.downloading_percentage),
					m_progressPercentage);
			m_smallIconResId = android.R.drawable.stat_sys_download;
		}

		int index = getCorrectIndex(isDownloadingEvent);
		if(index == ERROR){
			return ERROR;
		}
		m_resIdAppsList = m_appsListActionResIdsArray[index];
		return SUCCESS;
	}
	
	@Override
	protected Notification.Builder notificationHandler(Event ev, int flowId)
			throws CancelNotif {
		Log.d(LOG_TAG, "+notificationHandler");	
		
		if(initiate(ev) == ERROR){
			Log.d(LOG_TAG, "-notificationHandler=>initiate notification problems, return null");	
			return null;
		}
		Log.d(LOG_TAG, "notificationHandler=>Received event " + m_eventName);
		
		// Create strings and check if its new event or not
		String contentTitle = getContentTitle();
		String contentText = getContentText();
		String tickerText = getTickerText();
		boolean isDownloadingEvent = 
				m_eventName.equals(DMA_MSG_SCOMO_INSTALL_PROGRESS_UI) ? 
						false : true;

		if (m_notificationBuilder == null || !contentText.equals(m_contentText)
				|| !tickerText.equals(m_tickerText)|| !contentTitle.equals(m_contentTitle)) {
			m_contentText = contentText;
			m_tickerText = tickerText;
			m_contentTitle = contentTitle;
			
			m_notificationBuilder = new Notification.Builder(ctx)
					.setSmallIcon(m_smallIconResId)
					.setOngoing(true)
					.setAutoCancel(true)
					.setContentIntent(
							FlowManager.getReturnToFgIntent(ctx, flowId));
			
			m_notificationBuilder.setContentTitle(m_contentTitle);
			m_notificationBuilder.setContentText(m_contentText);
			m_notificationBuilder.setTicker(m_tickerText);		
		}
		Log.d(LOG_TAG, "notificationHandler=>ticker: " + m_tickerText);
		Log.d(LOG_TAG, "notificationHandler=>content title: " + m_contentTitle);
		Log.d(LOG_TAG, "notificationHandler=>content text: " + m_contentText);
	
		if (isDownloadingEvent) {// download
			Log.d(LOG_TAG, "notificationHandler=>displaying progress: "
					+ m_progressPercentage + "%");	
			m_notificationBuilder.setProgress(MAX_PROGRESS, m_progressPercentage,
					false);	
			m_notificationBuilder.setContentInfo(m_downloadingPercentage);
		}
		Log.d(LOG_TAG, "-notificationHandler");
		return m_notificationBuilder;
	}
}
