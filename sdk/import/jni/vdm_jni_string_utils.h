/*
 *******************************************************************************
 *
 * vdm_jni_string_utils.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_jni_string_utils.h
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#ifndef __VDM_JNI_STRING_UTILS_H__
#define __VDM_JNI_STRING_UTILS_H__

#ifdef __cplusplus
extern "C" {
#endif

// Get ASCII representation of string parameters.
#define VDM_JNI_jStringToNativeString(env, inJStr, inNativeStr)		\
	if (inJStr)									\
	{											\
		inNativeStr = (UTF8Str)(*env)->GetStringUTFChars(env, inJStr, JNI_FALSE); \
		if (!inNativeStr)						\
			return;	\
	}

// Release memory used to hold ASCII representation.
#define VDM_JNI_releaseNativeString(env, inJStr, inNativeStr)		\
	if (inJStr)									\
	{											\
		(*env)->ReleaseStringUTFChars(env, inJStr, (char*)inNativeStr); \
	}


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

