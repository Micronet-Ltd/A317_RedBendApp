/*
 *******************************************************************************
 *
 * vdm_jni_logcat.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_jni_logcat.h
 *
 * \brief OMA DM Protocol Engine SDK
 *******************************************************************************
 */

#ifndef __VDM_JNI_LOGCAT_H__
#define __VDM_JNI_LOGCAT_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>

/* convert VDM log level to Android log level */
#define VDM_TO_ANDROID_LOGLEVEL_Error       ANDROID_LOG_ERROR
#define VDM_TO_ANDROID_LOGLEVEL_Trace       ANDROID_LOG_INFO
#define VDM_TO_ANDROID_LOGLEVEL_Warning     ANDROID_LOG_WARN
#define VDM_TO_ANDROID_LOGLEVEL_Notice      ANDROID_LOG_INFO
#define VDM_TO_ANDROID_LOGLEVEL_Info        ANDROID_LOG_DEBUG
#define VDM_TO_ANDROID_LOGLEVEL_Debug       ANDROID_LOG_VERBOSE
#define VDM_TO_ANDROID_LOGLEVEL_Verbose     ANDROID_LOG_VERBOSE

#if !defined(PROD_MIN) || defined(DYNAMIC_LOGGING)
 #define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "vdm_native",__VA_ARGS__)
 #define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "vdm_native",__VA_ARGS__)
 #define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "vdm_native",__VA_ARGS__)
 #define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "vdm_native",__VA_ARGS__)
 #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "vdm_native",__VA_ARGS__) 
 #define LOGL(l, tag, ...) __android_log_print(l  , tag, __VA_ARGS__) 
 #define FUNCLOG LOGD("%s called", __FUNCTION__)
#else 
 #define LOGV (void)
 #define LOGD (void)
 #define LOGI (void)
 #define LOGW (void)
 #define LOGE (void)
 #define LOGL (void)
#define FUNCLOG LOGD (void)
#endif	//(PROD_MIN)


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

