/*
 *******************************************************************************
 *
 * dma_scomo_ins_actions.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_scomo_ins_actions.h
 *
 * \brief Scomo installation actions
 *******************************************************************************
 */

#ifndef DMA_SCOMO_INS_ACTIONS_H_
#define DMA_SCOMO_INS_ACTIONS_H_

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
	DMA_SCOMO_recoveryType_RB = 1,
	DMA_SCOMO_recoveryType_GOTA = 2,
} DMA_SCOMO_recoveryType_t;

#define RECOVERY_TYPE_NODE_URI ((UTF8CStr) "./DevDetail/Ext/RedBend/RecoveryType")
#define RECOVERY_TYPE_NODE_VALUE_GOTA "GOTA"
#define RECOVERY_TYPE_NODE_VALUE_RB "RB"

/*!
 *******************************************************************************
 * Install the DP.
 *
 * \param	msgId				SCOMO message
 * \param	inDpX				DP name
 * \param	inIsAfterReboot		A flag indicating if this call is a continuation
 * 								of installation which started before reboot
 * \param	inOperationType		Operation type (fumo, scomo, etc.)
 *
 * \return	0 on success, 1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_install(char *msgId, char *inDpX, int inIsAfterReboot,
	int inOperationType);

/*!
 *******************************************************************************
 * Complete the DP install.
 *
 * \param	msgId	SCOMO message
 *
 * \return	0
 *******************************************************************************
 */
int DMA_redbend_SCOMO_installDone(char *msgId);

/*!
 *******************************************************************************
 * Get the result of a DC install.
 *
 * \param	msgId		SCOMO message
 * \param	dpX			SCOMO DP name
 * \param	id			SCOMO DC id
 * \param	name		SCOMO DC name
 * \param	version		SCOMO DC version
 * \param	desc		SCOMO DC description
 * \param	envType		SCOMO DC environment type
 * \param	isActive	true if the DC should be installed active, false
 *						otherwise
 *
 * \return	0
 *******************************************************************************
 */
int DMA_redbend_SCOMO_installResult(char *msgId, char *dpX, char *id, char *name,
		char *version, char *desc, char *envType, unsigned int isActive, int inInstallResult);

/*!
 *******************************************************************************
 * Extract attached data at the end of DP
 *
 * \param	msgName          Event that caused this action to be executed
 * \param	googleUpdatePath Where to save the extracted additional data
 *
 * \return	Whether the operation succeeded (TRUE for success)
 *******************************************************************************
 */
int DMA_redbend_SCOMO_separateDP(char *msgName, char *googleUpdatePath);

/*!
 *******************************************************************************
 * Check what is the recovery type of the device
 *
 * \param	msgName          Event that caused this action to be executed
 *
 * \return	One of the values of DMA_SCOMO_recoveryType_t
 *******************************************************************************
 */
int DMA_redbend_SCOMO_checkRecoveryType(char *msgName);

/*!
 *******************************************************************************
 * Get the battery threshold
 *
 * \param	msgName          Event that caused this action to be executed
 *
 * \return	the battery threshold
 *******************************************************************************
 */
IU32 DMA_redbend_SCOMO_getBatteryThreshold(char *msgName);

/*!
 *******************************************************************************
 * Check if battery level of the device is enough
 *
 * \param	msgName          Event that caused this action to be executed
 * \param	batteryLevel     Battery level of the device
 *
 * \return	TRUE if the battery level is above or equal the threshold, FALSE otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_checkBatteryLevel(char *msgName, int batteryLevel);

/*!
 *******************************************************************************
 * Set the result code of component installation
 *
 * \param	inEventName          Event that caused this action to be executed
 * \param	inInstallResult      The result value (values are from swm_general_errors.h)
 *
 * \return	always 0
 *******************************************************************************
 */
int DMA_redbend_SCOMO_SetCompInstallResult(char *inEventName, int inInstallResult);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* DMA_SCOMO_INS_ACTIONS_H_ */
