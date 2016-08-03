/*
 *******************************************************************************
 *
 * vdm_client_pl_log_android.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_client_pl_log_android
 *
 * \brief	Extra APIs supporting Android LogCat tag field
 *
 *******************************************************************************
 */


#ifndef VDM_CLIENT_PL_ANDROID_H_ 	//!< Internal.
#define VDM_CLIENT_PL_ANDROID_H_

#ifdef __cplusplus
extern "C" {
#endif


/**
 * Set whether should log to file and the path to the log file in addition
 * to Android LogCat.
 *
 * @param isLogToFile 	TRUE if should log to file, otherwise FALSE
 * @param inPath		Full path to the log file.
 * 						If NULL and \a isLogToFile is TRUE then default
 * 						log path is used.
 * 						The parameter is ignored if \a isLogToFile is FALSE.
 */
void VDM_Client_PL_Log_Android_setIsLogToFile(IBOOL inIsLogToFile,
		const char *inPath);

/**
 * Set the default tag that will appear in LogCat messages.
 *
 * @param inTag Default tag
 */
void VDM_Client_PL_Log_Android_setDefaultTag(const char *inTag);

/**
 *  Log prefix to an upcoming log message.
 *
 * \note	This function is always followed by a call to
 * 			\ref VDM_Client_PL_Log_Android_logMsg.
 *
 * \param	inLevel		Android log level
 * \param	inFormat	Format string
 * \param	...			Arguments list
 *
 * \return	None
 */
void VDM_Client_PL_Android_logPrefix(IU32 inLevel, const char* inFormat, ...);

/**
 * Log message using the input tag.
 * NOTE: Implemented only using native log API. Not implemented using Java API
 *
 * @param 	inTag		LogCat tag
 * @param 	inMsg		LogCat message
 */
void VDM_Client_PL_Log_Android_logMsg(const char *inTag, const char *inMsg);



#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // VDM_CLIENT_PL_ANDROID_H_
