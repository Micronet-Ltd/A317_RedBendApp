/*
 *******************************************************************************
 *
 * dma_lawmo_actions.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	dma_lawmo_actions.h
 *
 * \brief	Reference Application LAWMO Actions
 *******************************************************************************
 */

#ifndef _DMA_FUMO_ACTIONS_H_
#define _DMA_FUMO_ACTIONS_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <vdm_smm_types.h>

/*!
 *******************************************************************************
 * Send LAWMO result to DM Server
 *
 * \param	msgId			Message id
 * \param	operationType	LAWMO operation type
 * \param	lawmoResult		LAWMO result
 *
 * \return	0 on success, non-zero on failure
 *******************************************************************************
 */
int DMA_redbend_LAWMO_triggerReportSession(char *msgId,
	unsigned int operationType, unsigned int lawmoResult);

/*!
 *******************************************************************************
 * Get if user mode requires the end-user to be notified about the LAWMO result.
 *
 * \param	msgId			Message id
 *
 * \return	0 if user should be notified, 1 otherwise
 *******************************************************************************
 */
int DMA_redbend_LAWMO_shouldNotifyUser(char *msgId);

/*!
 *******************************************************************************
 * Check if initiator is Lawmo.
 *
 * \param	msgId			Message id
 * \param	initiatorName	       Initiator name
 *
 * \return	1 if it is  Lawmo, 0 otherwise
 *******************************************************************************
 */
int DMA_redbend_LAWMO_isLawmoInitiator(char *msgId, char *initiatorName);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_FUMO_ACTIONS_H_ */

