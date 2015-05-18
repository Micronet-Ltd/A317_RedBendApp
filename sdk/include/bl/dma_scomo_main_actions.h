/*
 *******************************************************************************
 *
 * dma_scomo_main_actions.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_scomo_main_actions.h
 *
 * \brief Scomo main SM actions
 *******************************************************************************
 */

#ifndef _DMA_SCOMO_MAIN_ACTIONS_H_
#define _DMA_SCOMO_MAIN_ACTIONS_H_

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Prepare and send report about a SCOMO operation.
 *
 * \param	msgId	SCOMO/FUMO message
 * \param	result	SCOMO/FUMO operation result
 * \param	dpX		SCOMO DP name
 * \param   inOperationType	E_OperationType_Fumo or E_OperationType_FumoInScomo - trigger
 * 				FUMO report, E_OperationType_Scomo - trigger SCOMO report
 *
 * \return	0 if report session triggered successfully, 1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_triggerReport(char *inMsgId, unsigned int inResult,
	char *inDpX, unsigned int inOperationType);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_SCOMO_MAIN_ACTIONS_H_ */
