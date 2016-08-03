/*
 *******************************************************************************
 *
 * vdm_utl_logger.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_utl_logger.h
 *
 * \brief	Logging API
 *******************************************************************************
 */
#ifndef _VDM_UTL_LOGGER_H_
#define _VDM_UTL_LOGGER_H_      //!< Internal

#ifdef __cplusplus
extern "C" {
#endif

#include "vdm_client_pl_log_levels.h"

// This include file include macros that would normally be in the source
// file, but since macros must be defined in a header file, they are "hidden"
// for clarity. Normally the user would be interested in the Logger API only.
#include "vdm_utl_logger_inl.h"

#include <vdm_error.h>

/// @cond EXCLUDE

/*!
 *******************************************************************************
 * Callback invoked to get a component name.
 *
 * \param	id		Component id
 *
 * \return	The component name
 *******************************************************************************
 */
typedef const char *(*getComponentStringCB_t)(IU32 id);

/// @endcond

/*!
 *******************************************************************************
 * Initialize the logging utility.
 *
 * \param	getComponentString		Callback to get a component name, a
 *									\ref getComponentStringCB_t value
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_MEMORY if the system is out of
 *			memory
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_Logger_init(getComponentStringCB_t getComponentString);

#if defined(DYNAMIC_LOGGING) && defined(PROD_MIN)
/*!
 *******************************************************************************
 * Get the name of the log level.
 *
 * \param	inLevel		The log level numerical value
 *
 * \return	The log level name, "None" if the value is -1, or NULL if the value
 *			doesn't match any log level
 *******************************************************************************
 */
extern char *VDM_UTL_Logger_getLogLevelTypeStr(E_VDM_LOGLEVEL_TYPE inLevel);

/// @cond EXCLUDE

/*!
 *******************************************************************************
 * Allocate a log level variable inside the log context for the component, set
 * it to -1 (no logging for component) and return it for use as a context.
 *
 * \param	inComponentId	The component id
 * \param	outContext		The context returned by this function
 *
 * \return	VDM_ERR_OK on success, VDM_ERR_INVALID_CALL if the logging utility
 *			could not initialize, or VDM_ERR_MEMORY if there was a memory
 *			allocation error
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_Logger_initializeAndGetContextForLogComponentExternalStorage(
	IU32 inComponentId,
	E_VDM_LOGLEVEL_TYPE **outContext);
#endif

/// @endcond

/*!
 *******************************************************************************
 * Terminate the logging utility.
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_UTL_Logger_term(void);

// Enable logging if not building for PROD_MIN, enabling logging for a specific
// component, or if this file is included by the client application.
#if !defined(PROD_MIN) || defined(LOG_IN_RLS)

/*!
 *******************************************************************************
 * Set the default logging level for all components.
 *
 * \param	inLevel		The logging level, an \ref E_VDM_LOGLEVEL_TYPE value
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_INVALID_CALL if the logging
 *			utility was not initialized
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_Logger_setDefaultLevel(
	E_VDM_LOGLEVEL_TYPE inLevel);

/*!
 *******************************************************************************
 * Set the logging level for a specific component.
 *
 * \param	inComponentId	Component ID, a \ref VDM_componentType value
 * \param	inLevel			The logging level, an \ref E_VDM_LOGLEVEL_TYPE value
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_MEMORY if there was a memory
 *			allocation error
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_Logger_setComponentLevel(
	IU32 inComponentId,
	E_VDM_LOGLEVEL_TYPE inLevel);

/*!
 *******************************************************************************
 * Get the log file full path.
 *
 * \param	outFilePath	log file full path
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_UTL_logger_getFullPath(char **outFilePath);

/*!
 *******************************************************************************
 * Create log snapshot. The snapshot is one or more files that contain the
 * log.
 *
 * \param	inDstPath	The destination path where the files are written to.
 *
 * \return	VDM_ERR_OK on success. Error code otherwise.
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_Logger_snapshotCreate(char *inDstPath);

// inLogInfo format: <+|-><componentName>[=<level>]
//
// level values:
//	failure	- cannot be ignored
//	warning	- warning conditions
//  notice	- normal but significant - printed in default mode
//  info	- informational (printed in verbose mode)
//  debug	- debugging information
//	verbose	- verbose information
// IBOOL VDM_Logger_setComponent(char* inLogInfo);

/*
 * Logging API
 */

/*!
 * \addtogroup VDM_log_defs VDM Log Definitions
 *
 *******************************************************************************
 * The following macros provide a more user-friendly API for logging.
 * The format of "inMsg" is the standard printf arguments: (const char *
 * format, ...)
 *
 * USAGE: The format string and arguments must be enclosed with double
 * parentheses. For example:
 *
 *		VDM_log(("format", args...));
 *******************************************************************************
 * @{
 */

/*!
 * Usage:	VDM_logError(("format", args...));
 *			VDM_logError(("message"));
 */
#define VDM_logError(inMsg)         VDM_logLevel(E_VDM_LOGLEVEL_Error, inMsg)

/*!
 * Usage:	VDM_logWarning(("format", args...));
 *			VDM_logWarning(("message"));
 */
#define VDM_logWarning(inMsg)           VDM_logLevel(E_VDM_LOGLEVEL_Warning, inMsg)

/*!
 * Usage:	VDM_logNotice(("format", args...));
 *			VDM_logNotice(("message"));
 */
#define VDM_logNotice(inMsg)            VDM_logLevel(E_VDM_LOGLEVEL_Notice, inMsg)

/*!
 * Usage:	VDM_logInfo(("format", args...));
 *			VDM_logInfo(("message"));
 */
#define VDM_logInfo(inMsg)          VDM_logLevel(E_VDM_LOGLEVEL_Info, inMsg)

/*!
 * Usage:	VDM_logDebug(("format", args...));
 *			VDM_logDebug(("message"));
 */
#define VDM_logDebug(inMsg)         VDM_logLevel(E_VDM_LOGLEVEL_Debug, inMsg)

/*!
 * Usage:	VDM_logVerbose(("format", args...));
 *			VDM_logVerbose(("message"));
 */
#define VDM_logVerbose(inMsg)       VDM_logLevel(E_VDM_LOGLEVEL_Verbose, inMsg)

/*!
 * Log with the default level E_VDM_LOGLEVEL_Notice.
 *
 * Usage:	VDM_log(("format", args...));
 *			VDM_log(("message"));
 */
#define VDM_log(inMsg)              VDM_logNotice(inMsg)

/// @cond EXCLUDE

/*!
 * Dump hex data
 *
 * \note	This only dumps to the Debug or Verbose log levels.
 *
 * Usage: VDM_logDebugHex(buf, len); or
 *        VDM_logVerboseHex(buf, len);
 */
#define VDM_logDebugHex(buf, len)   \
	VDM_logDumpHex(E_VDM_LOGLEVEL_Debug, (#buf), (IU8 *)(buf), (len))
#define VDM_logVerboseHex(buf, len) \
	VDM_logDumpHex(E_VDM_LOGLEVEL_Verbose, (#buf), (IU8 *)(buf), (len))

/*
 * Dump formatted lines of 16 hex bytes. For example:
 * <pre>
 * 0000  48 54 54 50 20 70 6F 72 74 3D 38 32 33 36 20 68  HTTP port=8236 h
 * 0010  6F 73 74 3D 2A                                   ost=*
 * </pre>
 *
 * \note	This only dumps to the Debug log level.
 *
 * Usage: VDM_logDebugFormattedHex(buf, len);
 */
#define VDM_logDebugFormattedHex(buf, len)      VDM_logDumpFormattedHex(buf, len)

/// @endcond

/*!
 * Dump formatted lines of 16 hex bytes, preceded by a line of text. For example:
 * <pre>
 * Content of buffer is:
 * 0000  48 54 54 50 20 70 6F 72 74 3D 38 32 33 36 20 68  HTTP port=8236 h
 * 0010  6F 73 74 3D 2A                                   ost=*
 * </pre>
 *
 * \note	This only dumps to the Debug or Verbose log levels.
 *
 * Usage: VDM_logDebugFormattedHex2(msg, buf, len);
 *        VDM_logVerboseFormattedHex2(msg, buf, len);
 */
#define VDM_logDebugFormattedHex2(msg, buf, len)    \
	VDM_logDumpFormattedHex2(E_VDM_LOGLEVEL_Debug, msg, buf, len)

#define VDM_logVerboseFormattedHex2(msg, buf, len)  \
	VDM_logDumpFormattedHex2(E_VDM_LOGLEVEL_Verbose, msg, buf, len)

/* @} */

#else

#define VDM_logError(inMsg)
#define VDM_logWarning(inMsg)
#define VDM_logNotice(inMsg)
#define VDM_logInfo(inMsg)
#define VDM_logDebug(inMsg)
#define VDM_logVerbose(inMsg)
#define VDM_log(inMsg)
#define VDM_UTL_Logger_setDefaultLevel(level)
#define VDM_UTL_Logger_setComponentLevel(comp, level)

#define VDM_logDebugHex(buf, len)
#define VDM_logDebugFormattedHex(buf, len)
#define VDM_logDebugFormattedHex2(msg, buf, len)
#define VDM_logVerboseFormattedHex(buf, len)
#define VDM_logVerboseFormattedHex2(msg, buf, len)
#endif //PROD_MIN

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

