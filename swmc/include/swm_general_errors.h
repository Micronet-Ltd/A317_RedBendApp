/*
 *******************************************************************************
 *
 * swm_general_errors.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	swm_general_errors.h
 *
 * \brief	SWM Client Errors
 *******************************************************************************
 */
#ifndef _SWM_GENERAL_ERROR_H_
#define _SWM_GENERAL_ERROR_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <vdm_pl_types.h>

/*! SWM_Error */
typedef IS16 SWM_Error;

/*! Success */
#define SWM_ERR_OK									0x0000	/*     0 */

/*! Unspecific error */
#define SWM_ERR_UNSPECIFIC							0x0010	/*    16 */

/*! Routine called when not allowed or bad params */
#define SWM_ERR_INVALID_CALL						0x0012	/*    18 */

/*! SyncML message protocol or version error */
#define SWM_ERR_INVALID_PROTO_OR_VERSION			0x0020	/*	  32 */

/*! Supplied buffer isn't long enough */
#define SWM_ERR_BUFFER_OVERFLOW						0x6000	/* 24576 */

/*! Invalid ordinal number for installer type */
#define SWM_ERR_INVALID_ORD_NUM_FOR_INST_TYPE		0x7001	/* 28673 */

/*! No installer exists for that type */
#define SWM_ERR_NO_MATCHING_INSTALLER				0x7002

/*! Environment API failure */
#define SWM_ERR_ENV									0x0101

/*! Not enough memory */
#define SWM_ERR_MEMORY								0x0102

/*! Not enough privileges (ensure AEEPRIVID_PLFile, AEEPRIVID_ModInstaller) */
#define SWM_ERR_PRIVLEVEL							0x0103

/*! Component not initialized */
#define SWM_ERR_NOT_INITILIZED						0x0104

/*! Component already initialized */
#define SWM_ERR_ALREADY_INITILIZED					0x0105

/*! Bad input */
#define SWM_ERR_BAD_INPUT							0x0106

/*! Component not found */
#define SWM_ERR_COMP_MISSING						0x0107

/*! Installer currently busy */
#define SWM_ERR_BUSY								0x0108

/*! Invalid DP */
#define SWM_ERR_DP_FAILURE							0x0109

/*! User cancelled the operation */
#define SWM_ERR_CANCELLED							0x0110

/*! Attribute not supported */
#define SWM_ERR_ATTR_NOT_SUPPORT					0x0111

/*! File system space not available */
#define SWM_ERR_NO_FS_SPACE_AVAILABLE				0x0112

/*! External validation of the DP failed */
#define SWM_ERR_DP_EXT_VALIDATION_FAILED  			0x0113

/*! Uninstall failure */
#define SWM_UA_ERR_FAILED_TO_UNINSTALL_APK			0x0200

/*! Install failure */
#define SWM_UA_ERR_FAILED_TO_INSTALL_APK			0x0201

/*! Parsing error */
#define SWM_UA_ERR_PM_PARSING_ERROR					0x0203

/*! cannot open directory */
#define SWM_ERR_DIR_OPEN							0x0204

/*! mount failed */
#define SWM_ERR_MOUNT								0x0205

/*! UA errors */
/**************/
/*! UA error */
#define SWM_UA_ERR_BASE								0x0800

/*! UA: Illegal command */
#define SWM_UA_ERR_ILLEGAL_CMD						SWM_UA_ERR_BASE+0x1

/*! UA: Can't allocate memory */
#define SWM_UA_ERR_CANT_ALLOCATE_RAM				SWM_UA_ERR_BASE+0x2


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif
