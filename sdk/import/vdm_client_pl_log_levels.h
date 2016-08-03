/*
 *******************************************************************************
 *
 * vdm_client_pl_log_levels.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_client_pl_log_levels.h
 *
 * \brief	OMA DM Protocol Engine Logger Utility Severity Levels
 *******************************************************************************
 */
#ifndef VDM_CLIENT_PL_LOG_LEVELS_H
#define VDM_CLIENT_PL_LOG_LEVELS_H

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @addtogroup pl_log	Logging
 * @{
 */

/*!
 * Logging levels
 */
typedef enum {
	E_VDM_LOGLEVEL_Error = 1,	//!< Failure (always printed)
	E_VDM_LOGLEVEL_Trace,		//!< TRACE logs always printed)
	E_VDM_LOGLEVEL_Warning,		//!< Warning
	E_VDM_LOGLEVEL_Notice,		//!< Notice (default)
	E_VDM_LOGLEVEL_Info,		//!< Info
	E_VDM_LOGLEVEL_Debug,		//!< Debug
	E_VDM_LOGLEVEL_Verbose		//!< Verbose

} E_VDM_LOGLEVEL_TYPE;

/*! @} */


#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

