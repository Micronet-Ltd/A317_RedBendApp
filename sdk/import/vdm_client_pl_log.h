/*
 *******************************************************************************
 *
 * vdm_client_pl_log.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_client_pl_log.h
 *
 * \brief	Platform-specific Logging APIs
 *
 * The vDirect Mobile framework uses logging APIs to write log messages. The
 * implementation can write the messages to a log file, the console screen, or
 * both, as desired.
 *
 * \note	The vDirect Mobile framework is provided in one of two possible
 *			build modes: "Release" or "Trace". Release mode eliminates most
 *			logging messages in order to enhance performance. Trace mode
 *			includes full logging, and is recommended while developing the
 *			Client application.
 *******************************************************************************
 */
#ifndef VDM_CLIENT_PL_LOG_H
#define VDM_CLIENT_PL_LOG_H			//!< Internal

#include <vdm_pl_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @defgroup pl_log	Logging
 * @ingroup pl
 * @{
 */

/*!
 *******************************************************************************
 * Initialize platform-specific logging data and store logger context.
 *
 * This API sets the logging context, which can be stored in a global
 * pointer.
 *
 * The logging component is initialized by the Engine once when it starts up.
 *
 * \note	The vDirect Mobile API includes a context utility to allow the
 * 			Framework components to manage their context. When implementing the
 * 			logging component, do not use the context utility to store the
 * 			logger context, as this may cause undefined behavior.
 *
 * \param	inLoggerContext		Handle to the logger component context that
 * 								should be available for retrieval by the logger
 *
 * \return	TRUE on success, FALSE otherwise
 *******************************************************************************
 */
extern IBOOL VDM_Client_PL_logInit(VDM_Handle_t inLoggerContext);

/*!
 *******************************************************************************
 * Terminate logging. Free allocated data if needed.
 *
 * The Engine terminates the logging component once before it shuts down.
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_Client_PL_logTerm(void);

/*!
 *******************************************************************************
 * Get logger component context.
 *
 * \return	Handle to the context passed during initialization
 *******************************************************************************
 */
extern VDM_Handle_t VDM_Client_PL_logGetContext(void);

/*!
 *******************************************************************************
 * Log prefix to an upcoming log message.
 *
 * Logging is done in two steps:
 *
 * 1. The vDirect Mobile Logger utility calls this API. This allows the logging 
 *    component to add decorations to the text, such as color according to the
 *    severity level, a timestamp, and so on. Typical information also includes
 *    the thread id, file, line number, and component id from which the message
 *    is called.
 *
 * 2. vDirect Mobile calls \ref VDM_Client_PL_logMsg. This function writes the
 *    actual log message to the log console and/or log file.
 *
 * Both functions take arguments similar to printf: a format string followed by
 * a number of data arguments that get replaced within the string. For more
 * information on printf format flags, see the printf manual page.
 * 
 * \note	This function is always followed by a call to
 * 			\ref VDM_Client_PL_logMsg.
 *
 * \param	inLevel		The message's severity level, an
 * 						\ref E_VDM_LOGLEVEL_TYPE value<br>
 * 						The severity level allows you to assign different
 * 						display attributes, such as colors, according to
 * 						severity.
 * \param	inFormat	Format string
 * \param	...			Arguments list
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_Client_PL_logPrefix(IU32 inLevel, const char* inFormat, ...);

/*!
 *******************************************************************************
 * Log input message.
 *
 * Logging is done in two steps:
 *
 * 1. The vDirect Mobile Logger utility calls \ref VDM_Client_PL_logPrefix. This
 *    allows the logging component to add decorations to the text, such as
 *    color according to the severity level, a timestamp, and so on. Typical
 *    information also includes the thread id, file, line number, and component
 *    id from which the message is called.
 *
 * 2. vDirect Mobile calls this function. This function writes the actual log
 *    message to the log console and/or log file.
 *
 * Both functions take arguments similar to printf: a format string followed by
 * a number of data arguments that get replaced within the string. For more
 * information on printf format flags, see the printf manual page.
 * 
 * \note	This function is always preceded by a call to
 * 			\ref VDM_Client_PL_logPrefix. No extra decorations should be
 * 			added to the input message.
 *
 * \param	inFormat	Format string
 * \param	...			Arguments list
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_Client_PL_logMsg(const char* inFormat, ...);

extern void VDM_Client_PL_disableConsole(void);

/*!
 *	This function is for use by the client Porting Layer only.
 *	Other layers, such as MMI, Comm, Utilities, MOs, Extensions, and so on,
 *	use the VDM_UTL_Logger macros.
 */
#define VDM_Client_PL_log(inLevel, inMsg)		\
	do {										\
		VDM_Client_PL_logPrefix(inLevel, "");	\
		VDM_Client_PL_logMsg inMsg ;			\
	} while (0)			

/*! @} */

#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

