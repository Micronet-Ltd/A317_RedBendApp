/*
 *******************************************************************************
 *
 * vdm_client_pl_log.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_log.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <stdio.h>
#include <stdarg.h>
#include <string.h>

#include <vdm_pl_types.h>
#include <vdm_pl_alloc.h>
#include <vdm_client_pl_log_levels.h>
#include "vdm_client_pl_linux_logger.h"
#include <vdm_client_pl_log.h>

/* Context data used by util layer */
VDM_Handle_t s_hLoggerContext = NULL;

#define VDM_LOG_MSG_BUFF_SIZE 2048
#define VDM_LOG_PREFIX_BUFF_SIZE 128
static char s_prefixBuf[VDM_LOG_PREFIX_BUFF_SIZE];
static IU32 s_level = E_VDM_LOGLEVEL_Notice;

//=========================
// Implementation
//=========================

IBOOL VDM_Client_PL_logInit(VDM_Handle_t inLoggerContext)
{
	IBOOL isOK = FALSE;

	s_hLoggerContext = inLoggerContext;

	VDM_Client_PL_Linux_LogConsole_init();
	isOK = VDM_Client_PL_Linux_LogFile_init();

	return isOK;
}

void VDM_Client_PL_logTerm(void)
{
	VDM_Client_PL_Linux_LogFile_term();
	s_hLoggerContext = NULL;
}

VDM_Handle_t VDM_Client_PL_logGetContext(void)
{
	return s_hLoggerContext;
}

void VDM_Client_PL_logPrefix(IU32 inLevel, const char* format, ...)
{
	va_list	args;

	s_level = inLevel;
	va_start(args, format);
	vsnprintf(s_prefixBuf, sizeof(s_prefixBuf), format, args);
	va_end(args);
}

void VDM_Client_PL_logMsg(const char* format, ...)
{
	va_list	args;
	size_t writtenCount = 0;
	static char msgBuf[VDM_LOG_MSG_BUFF_SIZE];
	size_t prefixLen, msgBufLen;

	prefixLen = strlen(s_prefixBuf);
	msgBufLen = sizeof(msgBuf) - prefixLen;
	strcpy(msgBuf, s_prefixBuf);

	va_start(args, format);
	writtenCount = (size_t) vsnprintf(&msgBuf[prefixLen], msgBufLen, format, args);
	va_end(args);
	writtenCount += prefixLen;

	if (writtenCount > 0 && (writtenCount + 2 <= VDM_LOG_MSG_BUFF_SIZE) &&
		(msgBuf[writtenCount - 1] != '\n'))
	{
		msgBuf[writtenCount] = '\n';
		msgBuf[writtenCount + 1] = '\0';
	}

	VDM_Client_PL_Linux_LogDebug_output(s_level, msgBuf);
}

void VDM_Client_PL_disableConsole(void)
{
	VDM_Client_PL_Linux_LogDebug_disableConsole();
}

