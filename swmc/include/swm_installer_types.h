/*
 *******************************************************************************
 *
 * swm_installer_types.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file swm_installer_types.h
 *
 * \brief SWM Client Installer types
 *******************************************************************************
 */


#ifndef _SWM_INSTALLER_TYPES_H_
#define _SWM_INSTALLER_TYPES_H_


#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    IT_VRM = 0,
    IT_EXTERN = 1,
    IT_SYMBIAN = 2,
    IT_JAVA = 3,
    IT_BREW = 4,
    IT_LINUX = 5,
    IT_BP = 6,
    IT_BOOTLESS = 7,
    IT_CAB_RECREATION = 8,
    IT_FOTA = 9,
    IT_APK = 10,
    IT_SYSAPK = 11,
    IT_WIDGETS = 12,
/*  IT_SED = 13, */
    IT_ANDROID_FW = 14,
    IT_FILE = 15, /*used for device settings*/

    IT_USER_SAMPLE_MIN = 50,
    IT_USER_SAMPLE_MAX = 70,
    IT_FW = 100,
} SWM_InstallerType_t ;


#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _SWM_INSTALLER_TYPES_H_ */
