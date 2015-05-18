/*
 *******************************************************************************
 *
 * vdm_client_pl_linux_logger.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_linux_logger.h
 *
 * \brief Logging Utility.
 *******************************************************************************
 */

#ifndef _VDM_CLIENT_PL_LINUX_LOGGER_H_
#define _VDM_CLIENT_PL_LINUX_LOGGER_H_

#ifdef __cplusplus
extern "C" {
#endif

//Utility function to log to all debug output channels
// (e.g. console window, file, IDE's deubg output window, etc.)
void VDM_Client_PL_Linux_LogDebug_output(IU32 inLevel, const char* inStr);

//Prefix a line in debug output window and the log file with additional info
//to the upcoming message
//(thread id, time stamp)
void VDM_Client_PL_Linux_LogDebug_Prefix(IU32 inLevel, IBOOL inConsoleOutput);

void VDM_Client_PL_Linux_LogDebug_disableConsole(void);

/*
 *******************************************************************************
 * Console window output.
 *
 * Error messages will appear in RED
 * Warning messages will appear in WHITE
 * Diagnostics messages will appear in GREEN
 *******************************************************************************
 */
void VDM_Client_PL_Linux_LogConsole_init(void);
void VDM_Client_PL_Linux_LogConsole_term(void);

/*
 *******************************************************************************
 * Log file.
 *
 *******************************************************************************
 */
IBOOL VDM_Client_PL_Linux_LogFile_init(void);
void VDM_Client_PL_Linux_LogFile_output(const char* inStr, IBOOL inForceNewline);
void VDM_Client_PL_Linux_LogFile_term(void);

#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

