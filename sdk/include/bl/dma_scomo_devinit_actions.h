/*
 *******************************************************************************
 *
 * dma_scomo_devinit_actions.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	dma_scomo_devinit_actions.h
 *
 * \brief	Reference Application SCOMO device-initiated actions
 *******************************************************************************
 */
#ifndef _DMA_DEVINIT_ACTIONS_H_
#define _DMA_DEVINIT_ACTIONS_H_

#ifdef __cplusplus
extern "C" {
#endif


/*!
 *******************************************************************************
 * Get the value of the ./Ext/RedBend/PollingIntervalInHours leaf.
 *
 * \param	inMsgId			SCOMO message
 *
 * \return	The value of the leaf converted to seconds if it exists and is valid
 *			(integer > 0), -1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_getPollingIntervalFromTree(char *inMsgId);

/*!
 *******************************************************************************
 * Replace the value of the ./Ext/RedBend/PollingIntervalInHours leaf.
 *
 * \param	inMsgId			SCOMO message
 * \param	inInterval		Interval
 *
 * return	0 if the value is replaced successfully, -1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_replacePollingIntervalInTree(char *inMsgId, unsigned int inInterval);

/*!
 *******************************************************************************
 * Get the value of the ./Ext/RedBend/BootupPollingInterval leaf.
 *
 * \param	inMsgId			SCOMO message
 *
 * \return	The value of the leaf converted to seconds if it exists and is valid
 *			(integer > 0), -1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_getBootupIntervalFromTree(char *inMsgId);

/*!
 *******************************************************************************
 * Get the value of the ./Ext/RedBend/RecoveryPollingInterval leaf.
 *
 * \param	inMsgId			SCOMO message
 *
 * \return	The value of the leaf converted to seconds if it exists and is valid
 * 			(integer > 0), -1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_getRecoveryIntervalFromTree(char *inMsgId);

/*!
 *******************************************************************************
 * Get a pseudo-random number in the range of 1 to \a inRangeEnd, for use as the
 * alarm interval.
 *
 * \param	inMsgId			SCOMO message
 * \param	inRangeEnd		The maximum number used for generating the
 * 							pseudo-random number
 *
 * \return	Random number in the range of 1 to \a inRangeEnd
 *******************************************************************************
 */
int DMA_redbend_SCOMO_getRandomInterval(char *inMsgId, unsigned int inRangeEnd);

/*!
 *******************************************************************************
 * Check if the value of interval is correct.
 *
 * \param	inMsgId			SCOMO message
 * \param	inInterval		Interval
 *
 * \return	0 if the value of inteval is above 0, -1 otherwise 
 *******************************************************************************
 */
int DMA_redbend_SCOMO_checkPollingInterval(char *inMsgId, signed int inInterval);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_DEVINIT_ACTIONS_H_ */
