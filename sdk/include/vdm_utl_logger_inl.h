/*
 *******************************************************************************
 *
 * vdm_utl_logger_inl.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/// @cond EXCLUDE

#ifdef __cplusplus
extern "C" {
#endif

/*
 *******************************************************************************
 * \file vdm_utl_logger_inl.h
 *
 * \brief	This file hides macros that are of no interest
 *			to the logger user, leaving vdm_utl_logger.h with only the API
 *
 * !!! This file should not appear in the doxygen documentation !!!
 *******************************************************************************
 */
#ifndef _VDM_UTL_LOGGER_H_
#error "vdm_utl_logger_inl.h must be included from vdm_utl_logger only"
#endif

#ifdef _VDMC_
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_CORE) && defined(LOG_CORE_MSGQ)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_UTIL) && defined(LOG_UTIL)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_MMI) && defined(LOG_MMI)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_COMM) && defined(LOG_COMM)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_DEVICE) && defined(LOG_DEVICE)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM) && defined(LOG_CORE_ENG)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_TRG) && defined(LOG_CORE_NIA)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_SESS) && defined(LOG_CORE_SESS)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_SESSQ) && defined(LOG_CORE_SESSQ)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_COMM) && defined(LOG_CORE_COMM)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_SYNCML) && defined(LOG_SYNCML)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_TREE) && defined(LOG_CORE_TREE)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_AUTH) && defined(LOG_CORE_AUTH)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_WBXML) && defined(LOG_CORE_WBXML)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_RDM_DL) && defined(LOG_CORE_DL)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_HTTP) && defined(LOG_HTTP)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_CLIENT) && defined(LOG_DMC)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_FUMO) && defined(LOG_FUMO)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_SCOMO) && defined(LOG_SCOMO)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_BOOTSTRAP) && defined(LOG_BOOTSTRAP)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_DS) && defined(LOG_DS)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_CONNMO) && defined(LOG_CONNMO)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_WSP) && defined(LOG_WSP)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_LAWMO) && defined(LOG_LAWMO)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

#if defined(VDM_COMPONENT_ID_E_VDM_COMPONENT_DL) && defined(LOG_ADL)
#undef LOG_IN_RLS
#define LOG_IN_RLS
#endif

/* Enable logging if not building for PROD_MIN,
 * A matching macro to the log component's value was used in build
 * or if this file is included by the client application. */
#if !defined(PROD_MIN) || defined(LOG_IN_RLS)

#include <vdm_client_pl_log.h>
#include <vdm_pl_string.h>

// Logger's "local" functions
IBOOL VDM_UTL_Logger_isAboveThreshold(IU32 inComponentId,
	E_VDM_LOGLEVEL_TYPE inLevel);

void VDM_UTL_Logger_lock(void);
void VDM_UTL_Logger_unlock(void);
void VDM_UTL_Logger_dumpHex(char *inBufName, IU8 *inBuf, IU32 inBufLen);
void VDM_UTL_Logger_dumpFormattedHex(const void *inData, IU32 inLength);
void VDM_UTL_Logger_dumpFormattedHex2(const char *inMsg, const void *inData,
	IU32 inLength);

extern const char *VDM_UTL_Logger_getComponentString(IU32 id);

// Used to limit the size of some printouts
#define VDM_LOG_ITEM_PRINTOUT_LEN 100

/* If file name is longer than 20, use only last 20 chars. */
#define VDM_debugFILECHARACTERS 20
#define __VDMFILE__ (VDM_PL_strlen(__FILE__) > VDM_debugFILECHARACTERS ? \
	__FILE__ + VDM_PL_strlen(__FILE__) - VDM_debugFILECHARACTERS : __FILE__)

/* This macro is used to extract the component id implicitly.
 * VDM_COMPONENT_ID should be defined in each component's standard header,
 * assigning it the enum value of the component (see \ref E_VDM_COMPONENT_TYPE in
 * vdm_components.h).
 *
 * For example, for FUMO component:
 * in file vdm_fumo_std.h:
 * #define VDM_COMPONENT_ID	(E_VDM_COMPONENT_FUMO)
 */

#define VDM_logResult(result, msg)                                          \
	do {                                                                    \
		E_VDM_LOGLEVEL_TYPE level; \
		level = ((result) == VDM_ERR_OK) ? E_VDM_LOGLEVEL_Debug : E_VDM_LOGLEVEL_Error; \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold((VDM_COMPONENT_ID), level))  \
		{                                                               \
			VDM_Client_PL_logPrefix(level, "%s.%5u: [%s] ",                 \
				__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
			VDM_Client_PL_logMsg msg;                                  \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#define VDM_logLevel(level, msg)                                            \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold((VDM_COMPONENT_ID), level))  \
		{                                                               \
			VDM_Client_PL_logPrefix(level, "%s.%5u: [%s] ",                 \
				__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
			VDM_Client_PL_logMsg msg;                                  \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

/* VDM_logDumpHex */
#define VDM_logDumpHex(inLevel, inBufName, inBuf, inLen)                    \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold((VDM_COMPONENT_ID), (inLevel)))  \
		{                                                               \
			VDM_Client_PL_logPrefix(E_VDM_LOGLEVEL_Debug, "%s.%5u: [%s] ",  \
				__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
			VDM_UTL_Logger_dumpHex(inBufName, inBuf, inLen);            \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

/* VDM_logDumpFormattedHex */
#define VDM_logDumpFormattedHex(inBuf, inLen)                               \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold((VDM_COMPONENT_ID), (E_VDM_LOGLEVEL_Debug))) \
		{                                                               \
			VDM_Client_PL_logPrefix(E_VDM_LOGLEVEL_Debug, "%s.%5u: [%s]",   \
				__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
			VDM_UTL_Logger_dumpFormattedHex(inBuf, inLen);              \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#define VDM_logDumpFormattedHex2(inLevel, inMsg, inBuf, inLen)              \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold((VDM_COMPONENT_ID), (inLevel)))  \
		{                                                               \
			VDM_Client_PL_logPrefix(E_VDM_LOGLEVEL_Debug, "%s.%5u: [%s]",   \
				__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
			VDM_UTL_Logger_dumpFormattedHex2(inMsg, inBuf, inLen);      \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#define VDM_logTRACE(msg)                                                   \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		VDM_Client_PL_logPrefix(E_VDM_LOGLEVEL_Trace, "TRACE: %s, %5u, [%s], ", \
			__VDMFILE__, __LINE__, VDM_UTL_Logger_getComponentString(VDM_COMPONENT_ID)); \
		VDM_Client_PL_logMsg msg;                                      \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#ifdef __ANDROID__
//Used for Java code (no preprocessor for file, line, and component ID)
//note: must #include <vdm_client_pl_log_android.h>

#define VDM_logJavaMsgComponent(level, component, tag, msg)                 \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		if (VDM_UTL_Logger_isAboveThreshold(component, level))           \
		{                                                               \
			VDM_Client_PL_Android_logPrefix(level, "%s: [%s] ",         \
				tag, VDM_UTL_Logger_getComponentString(component));     \
			VDM_Client_PL_Log_Android_logMsg(tag, msg);                 \
		}                                                               \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#define VDM_logJavaMsg(level, tag, msg)                                     \
	do {                                                                    \
		VDM_UTL_Logger_lock();                                              \
		VDM_Client_PL_Android_logPrefix(level, "%s: ", tag);                \
		VDM_Client_PL_Log_Android_logMsg(tag, msg);                     \
		VDM_UTL_Logger_unlock();                                            \
	} while (0)

#endif  //__ANDROID__

#else
#define VDM_logResult(result, msg)
#define VDM_logLevel(level, msg)
#define VDM_logDumpHex(name, buf, size)
#define VDM_logDumpFormattedHex(buf, size)
#define VDM_logDumpFormattedHex2(level, msg, buf, size)
#define VDM_logTRACE(msg)
#define VDM_logJavaMsg(level, tag, msg)
#define VDM_logJavaMsgComponent(level, component, tag, msg)

#endif //PROD_MIN

#ifdef __cplusplus
} /* extern "C" */
#endif

/// @endcond

