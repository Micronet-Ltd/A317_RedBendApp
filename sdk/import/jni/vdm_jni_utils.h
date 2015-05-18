/*
 *******************************************************************************
 *
 * vdm_jni_utils.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_jni_utils.h
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#ifndef __VDM_JNI_UTILS_H__
#define __VDM_JNI_UTILS_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

jint JNU_SetJVM(JavaVM *jvm);
JNIEnv *JNU_GetEnv();

// Throw a com.redbend.android.RbException if inError != VDM_ERR_OK
void JNU_throw(VDM_Error inError);

// Throw any Java Throwable (Error, Exception or RuntimeException)
void JNU_throwJavaException(const char *inClassName, const char *inMsg);

//Macro for throwing Java's OutOfMemoryError
#define JNU_throwOutOfMemoryError(msg)	\
	JNU_throwJavaException("java/lang/OutOfMemoryError", msg)


VDM_Error JNU_handleException(JNIEnv *env);

jobjectArray JNU_NewStringArray(JNIEnv *env, jint inSize);

//Note: Does not check validity of index
VDM_Error JNU_GetStringArrayElement(JNIEnv* env, jobjectArray inArray, 
	jint inIndex, UTF8CStr* outStr);

IBOOL JNU_checkUtfString(JNIEnv* env, const char* bytes);

void JNU_AttachCurrentThread(void);
void JNU_DetachCurrentThread(void);

/* Invoke an instance method by name.
 */
JNIEXPORT jvalue JNICALL
JNU_CallMethodByName(JNIEnv *env,
		     jboolean *hasException,
		     jobject obj,
		     const char *name,
		     const char *signature,
		     ...);

JNIEXPORT jvalue JNICALL
JNU_GetFieldByName(JNIEnv *env,
		   jboolean *hasException,
		   jobject obj,
		   const char *name,
		   const char *sig);
JNIEXPORT jobject JNICALL
JNU_NewObjectByName(JNIEnv *env, const char *class_name,
					const char *constructor_sig, ...);

JNIEXPORT jvalue JNICALL
JNU_CallMethodByNameV(JNIEnv *env,
					  jboolean *hasException,
					  jobject obj,
					  const char *name,
					  const char *signature,
					  va_list args);

/*
 * Convert a jstring to a utf8 string and strdup it.
 *
 * Note: does not check for NULL inString. if NULL is returned, then
 * OutOfMemoryException has been thrown.
 */
UTF8CStr JNU_getUtf8StringDup(JNIEnv *env, jstring inString);

/*
 * Check whether a non-NULL weak global reference still points to a live object.
 *
 * Returns:
 * 	TRUE if inWeakObj still refers to a live object
 * 	FALSE if inWeakObj refers to an object that has already been collected
 */
IBOOL JNU_isLiveObject(JNIEnv *env, jweak inWeakObj);

/*
 * Return IPL.java class global reference
 */
jclass JNU_GetIplCls();

/*
 * Store IPL.java class global reference
 */
void JNU_StoreIplCls();

/*
 * Delete IPL.java class global reference
 */
void JNU_DeleteIplCls(JNIEnv *env);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

