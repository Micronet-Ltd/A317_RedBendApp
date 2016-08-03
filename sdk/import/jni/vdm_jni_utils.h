/*
 *******************************************************************************
 *
 * vdm_jni_utils.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_jni_utils.h
 *
 * \brief OMA DM Protocol Engine SDK
 *******************************************************************************
 */

#ifndef __VDM_JNI_Utils_H__
#define __VDM_JNI_Utils_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

jint JNI_Utils_SetJVM(JavaVM *jvm);
JNIEnv *JNI_Utils_GetEnv(void);

// Throw a com.redbend.android.RbException if inError != VDM_ERR_OK
void JNI_Utils_throw(VDM_Error inError);

// Throw any Java Throwable (Error, Exception or RuntimeException)
void JNI_Utils_throwJavaException(const char *inClassName, const char *inMsg);

//Macro for throwing Java's OutOfMemoryError
#define JNI_Utils_throwOutOfMemoryError(msg)	\
	JNI_Utils_throwJavaException("java/lang/OutOfMemoryError", msg)


VDM_Error JNI_Utils_handleException(JNIEnv *env);

jobjectArray JNI_Utils_NewStringArray(JNIEnv *env, jint inSize);

//Note: Does not check validity of index
VDM_Error JNI_Utils_GetStringArrayElement(JNIEnv* env, jobjectArray inArray, 
	jint inIndex, UTF8CStr* outStr);

IBOOL JNI_Utils_checkUtfString(JNIEnv* env, const char* bytes);

void JNI_Utils_AttachCurrentThread(void);
void JNI_Utils_DetachCurrentThread(void);

/* Invoke an instance method by name.
 */
JNIEXPORT jvalue JNICALL
JNI_Utils_CallMethodByName(JNIEnv *env,
		     jboolean *hasException,
		     jobject obj,
		     const char *name,
		     const char *signature,
		     ...);

JNIEXPORT jvalue JNICALL
JNI_Utils_GetFieldByName(JNIEnv *env,
		   jboolean *hasException,
		   jobject obj,
		   const char *name,
		   const char *sig);
JNIEXPORT jobject JNICALL
JNI_Utils_NewObjectByName(JNIEnv *env, const char *class_name,
					const char *constructor_sig, ...);

JNIEXPORT jvalue JNICALL
JNI_Utils_CallMethodByNameV(JNIEnv *env,
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
UTF8CStr JNI_Utils_getUtf8StringDup(JNIEnv *env, jstring inString);

/*
 * Check whether a non-NULL weak global reference still points to a live object.
 *
 * Returns:
 * 	TRUE if inWeakObj still refers to a live object
 * 	FALSE if inWeakObj refers to an object that has already been collected
 */
IBOOL JNI_Utils_isLiveObject(JNIEnv *env, jweak inWeakObj);

/*
 * Return IPL.java class global reference
 */
jclass JNI_Utils_GetIplCls(void);

/*
 * Store IPL.java class global reference
 */
void JNI_Utils_StoreIplCls(void);

/*
 * Delete IPL.java class global reference
 */
void JNI_Utils_DeleteIplCls(JNIEnv *env);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

