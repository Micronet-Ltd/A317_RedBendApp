/*
 *******************************************************************************
 *
 * dma_scomo_sync_actions.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_scomo_sync_actions.h
 *
 * \brief Scomo Synchronization actions
 *******************************************************************************
 */

#ifndef _DMA_SCOMO_SYNC_ACTIONS_H_
#define _DMA_SCOMO_SYNC_ACTIONS_H_

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Remove SCOMO inventory in preparation for synchronizing the inventory with
 * the device's inventory.
 *
 * \param	msgId	SCOMO message
 *
 * \return	0 on success, 1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_removeAllComps(char *msgId);

/*!
 *******************************************************************************
 * Add the next item from the device inventory to the SCOMO inventory. This
 * function is called iteratively until all items are added (at which point it
 * returns 0.
 *
 * \param	msgId	SCOMO message
 *
 * \return	0 if there are no more items to add, 1 if there are more items
 *******************************************************************************
 */
int DMA_redbend_SCOMO_sync(char *msgId);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_SCOMO_SYNC_ACTIONS_H_ */
