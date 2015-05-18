/*
 *******************************************************************************
 *
 * vdm_client_pl_linux_logger.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_linux_logger.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <sys/types.h>
#ifndef VDM_QNX
#include <sys/syscall.h>
#endif
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <errno.h>
#include <pthread.h>
#include <vdm_error.h>
#include <vdm_pl_string.h>
#include <vdm_pl_string_utils.h>
#include <vdm_pl_mutex.h>
#include <vdm_pl_alloc.h>
#include <vdm_client_pl_log_levels.h>
#include <vdm_client_pl_log.h>
#include "vdm_client_pl_linux_logger.h"
#include "vdm_client_pl_linux_file.h"
#if defined(VDM_RUN_ON_ANDROID) || defined(VDM_USE_ANDROID_LOGCAT)
#include <vdm_jni_logcat.h>
#endif

//=========================
// types and definitions
//=========================

#define LOG_FILE_NAME "vdm"
#define LOG_FILE_SUFFIX ".log"

//File output
static FILE *s_hLogFile = NULL;
static IU32 s_fileSize;

#ifdef VDM_RUN_ON_ANDROID
#define LOG_FILE_PATH "/data/data/com.redbend.client/files"
#elif defined VDM_QNX
#define LOG_FILE_PATH "/var/log/omadm"
#else
#define LOG_FILE_PATH "/tmp"
#endif

#define LOG_FILE_MAX_SIZE 1048576
#define NUMBER_OF_SAVED_FILES 4

#define LOG_FULL_NAME LOG_FILE_PATH "/" LOG_FILE_NAME LOG_FILE_SUFFIX

static VDM_Handle_t s_hMutex = NULL;
const char *g_VDMLogFilePath = NULL;
const char *g_VDMLogDirPath = NULL;

//Console output
static int s_consoleDisabled = 0;
#if !defined(VDM_RUN_ON_ANDROID) && !defined(VDM_USE_ANDROID_LOGCAT)
static void VDM_Client_PL_Linux_LogConsole_output(IU32 inLevel, const char *inStr);
#endif
//=========================
// Implementation
//=========================

void VDM_Client_PL_Linux_LogDebug_output(IU32 inLevel, const char *inStr)
{
	if (inLevel != E_VDM_LOGLEVEL_Trace)
		VDM_Client_PL_Linux_LogDebug_Prefix(inLevel, !s_consoleDisabled);

#if defined(VDM_RUN_ON_ANDROID) || defined(VDM_USE_ANDROID_LOGCAT)
	{
		//convert VDM log level to Android log level
		IS32 priority = VDM_TO_ANDROID_LOGLEVEL_Notice;
		switch (inLevel)
		{
			case E_VDM_LOGLEVEL_Error:   priority = VDM_TO_ANDROID_LOGLEVEL_Error; break;
			case E_VDM_LOGLEVEL_Trace:   priority = VDM_TO_ANDROID_LOGLEVEL_Trace; break;
			case E_VDM_LOGLEVEL_Warning: priority = VDM_TO_ANDROID_LOGLEVEL_Warning; break;
			case E_VDM_LOGLEVEL_Notice:  priority = VDM_TO_ANDROID_LOGLEVEL_Notice; break;
			case E_VDM_LOGLEVEL_Info:    priority = VDM_TO_ANDROID_LOGLEVEL_Info; break;
			case E_VDM_LOGLEVEL_Debug:   priority = VDM_TO_ANDROID_LOGLEVEL_Debug; break;
			case E_VDM_LOGLEVEL_Verbose: priority = VDM_TO_ANDROID_LOGLEVEL_Verbose; break;
			default: priority = VDM_TO_ANDROID_LOGLEVEL_Notice; break;
		}
		LOGL(priority, "vDM", inStr);
	}
#else
	//Write to console window
	if (!s_consoleDisabled)
		VDM_Client_PL_Linux_LogConsole_output(inLevel, inStr);
#endif
	//Write to log file
	VDM_Client_PL_Linux_LogFile_output(inStr, FALSE);
}

void VDM_Client_PL_Linux_LogDebug_disableConsole(void)
{
	s_consoleDisabled = 1;
}

void VDM_Client_PL_Linux_LogDebug_Prefix(IU32 inLevel, IBOOL inConsoleOutput)
{
    time_t t;
	char timestr[128];
	char prefix[1024];
	IU32 threadid;
	char *levelCode = "NETWNIDV";

	if (inLevel > E_VDM_LOGLEVEL_Debug)
		inLevel = E_VDM_LOGLEVEL_Verbose;

	//get tid using a system call. (gettid() is distribution dependent, and not always implemented
#ifdef __ANDROID__
	threadid = (IU32)syscall(__NR_gettid);
#elif defined VDM_QNX
	threadid = pthread_self();
#else
	threadid = (IU32)syscall(SYS_gettid);
#endif

	tzset();
    time(&t);
    VDM_PL_strncpy(timestr, ctime(&t), sizeof(timestr));
	timestr[sizeof(timestr)-1]='\0';
    timestr[VDM_PL_strlen(timestr)-1] = '\0'; // chomp

	VDM_PL_snprintf(prefix, sizeof(prefix), "%s _%c_ (%ld) ", timestr,
		levelCode[inLevel], threadid);

	//Write prefix to log file
	VDM_Client_PL_Linux_LogFile_output(prefix, FALSE);

	//prefix to console window
	if (inConsoleOutput)
		printf("%s", prefix);
}

// -- console --

void VDM_Client_PL_Linux_LogConsole_init(void)
{
}

#if !defined(VDM_RUN_ON_ANDROID) && !defined(VDM_USE_ANDROID_LOGCAT)
static void VDM_Client_PL_Linux_LogConsole_output(IU32 inLevel,
	const char *inStr)
{
	if (inLevel == E_VDM_LOGLEVEL_Error)
		printf("\033[31m"); /* red */
	else if (inLevel == E_VDM_LOGLEVEL_Warning)
		printf("\033[1m"); /* dark black */
	else if (inLevel == E_VDM_LOGLEVEL_Trace)
		printf("\033[35m"); /* purple */
	else if (inLevel < E_VDM_LOGLEVEL_Error) //Diagnostics message
		printf("\033[32m"); /* green */

	// Print to screen
	printf("%s", inStr);

	//reset console attributes
	if (inLevel < E_VDM_LOGLEVEL_Notice)
		printf("\033[0m"); /* white */
}
#endif

void VDM_Client_PL_Linux_LogConsole_term(void)
{
	// nothing to do.
}

static VDM_Error _mkdir(const char *inFolderPath, const mode_t inMode)
{
	int rc;

#ifdef DEBUG_PRINTF
	printf("inFolderPath: %s\n", inFolderPath);
#endif
	rc = mkdir(inFolderPath, inMode);
#ifdef DEBUG_PRINTF
	printf("mkdir result: %d errno: %d\n", rc, errno);
#endif

	return !rc || errno==EEXIST ? VDM_ERR_OK : VDM_ERR_UNSPECIFIC;
}

static VDM_Error recursiveMkdir(const char *inFolderPath, const mode_t inMode)
{
	VDM_Error ret = VDM_ERR_OK;
	char *path = NULL;
	char *slash = NULL;

#ifdef DEBUG_PRINTF
	printf(" %s path: %s\n", __func__, inFolderPath);
#endif

	if (!inFolderPath)
	{
		ret = VDM_ERR_BAD_INPUT;
		goto end;
	}

	path = VDM_PL_malloc(VDM_PL_strlen(inFolderPath)+1);
	if (!path)
	{
		ret = VDM_ERR_MEMORY;
		goto end;
	}
	VDM_PL_strcpy(path, inFolderPath);

	slash = path;

	while ((slash = VDM_PL_strchr(slash+1, '/')))
	{
		*slash = '\0';

		ret = _mkdir(path, inMode);
		if (ret != VDM_ERR_OK)
			break;
		
		*slash = '/';
	}

	ret = _mkdir(inFolderPath, inMode);

end:
	VDM_PL_freeAndNullify(path);
	return ret;
}

IBOOL VDM_Client_PL_Linux_LogFile_init()
{
	IBOOL isOK = TRUE;
	const char *fileFullName = g_VDMLogFilePath ? g_VDMLogFilePath : LOG_FULL_NAME;
	const char *directoryPath = g_VDMLogDirPath ? g_VDMLogDirPath : LOG_FILE_PATH;

	// Create the folder if needed
	recursiveMkdir(directoryPath, 0777);

	VDM_Client_linux_fopen(fileFullName, "w+", &s_hLogFile);
	s_fileSize = 0;

	if (!s_hLogFile)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Error, ("!!! The file '%s' cannot be opened for debug messages, %m", fileFullName));
		isOK = FALSE;
	}
	else
	{
		fputs( "\n\n", s_hLogFile );
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Info, ("************** Logging messages will output to file '%s' **************", fileFullName));
	}

	s_hMutex = VDM_PL_Mutex_create();
	if (!s_hMutex)
	{
		VDM_Client_PL_log(E_VDM_LOGLEVEL_Warning, ("!!! Warning - Failed to create mutex for log file!", fileFullName));
		isOK = FALSE;
	}

	return isOK;
}

void VDM_Client_PL_Linux_LogFile_term(void)
{
	if (s_hLogFile)
	{
		/* Close debug stream */
		VDM_Client_linux_fclose(&s_hLogFile);

		if (s_hMutex)
		{
			VDM_PL_Mutex_close( s_hMutex );
			s_hMutex = NULL;
		}
	}
}

static VDM_Error cycleLogs(void)
{
	char src[1024] = {'\0'};
	char dst[1024] = {'\0'};
	int i = 0;
	const char *fileFullName = g_VDMLogFilePath ? g_VDMLogFilePath : LOG_FULL_NAME;
	const char *directoryPath = g_VDMLogDirPath ? g_VDMLogDirPath : LOG_FILE_PATH;

	/* close the log file */
	VDM_Client_linux_fclose(&s_hLogFile);

	/* Move all the files even if do not exist */
	for (i = NUMBER_OF_SAVED_FILES-1; i ; i--)
	{
		VDM_PL_snprintf(src, sizeof(src), "%s/%s%02d%s", directoryPath,
			LOG_FILE_NAME, i, LOG_FILE_SUFFIX);
		VDM_PL_snprintf(dst, sizeof(dst), "%s/%s%02d%s", directoryPath,
			LOG_FILE_NAME, i+1, LOG_FILE_SUFFIX);
		rename(src, dst);
	}

	/* Use the last src (0) and take the orig file name */
	rename(fileFullName, src);

	/* open the log file again */
	s_fileSize = 0;
	return VDM_Client_linux_fopen(fileFullName, "w+", &s_hLogFile);
}

void VDM_Client_PL_Linux_LogFile_output(const char *inStr, IBOOL inForceNewline)
{
	IU32 len = VDM_PL_strlen(inStr);

	//Write to log file
	if (s_hLogFile && s_hMutex)
	{
		VDM_PL_Mutex_lock( s_hMutex );

		if (s_fileSize + len > LOG_FILE_MAX_SIZE)
			cycleLogs();
		
		if (s_hLogFile) // Avoid crash if open of file fails
		{
			fputs(inStr, s_hLogFile);
		    if (inForceNewline)				//note: currently TRUE only in Android
			    fputc('\n', s_hLogFile);
			fflush(s_hLogFile);
			s_fileSize += len;
		}

		VDM_PL_Mutex_unlock( s_hMutex );
	}
}

