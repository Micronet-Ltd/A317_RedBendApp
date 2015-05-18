/*
 *******************************************************************************
 *
 * dma_scomo_vsense_server_attribute_change_action.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_scomo_vsense_server_attribute_change_action.h
 *
 * \brief Server attributes changes
 *******************************************************************************
 */

#ifndef _DMA_SCOMO_VSENSE_SERVER_ATTRIBUTE_CHANGE_ACTIONS_H
#define _DMA_SCOMO_VSENSE_SERVER_ATTRIBUTE_CHANGE_ACTIONS_H

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Read vSense server url, domain name and polling values from tree.xml
 *
 * \param	msgId	SCOMO message
 *
 * \return	0 on success, 1 otherwise
 *******************************************************************************
 */
int DMA_redbend_SCOMO_getvSenseAttributesFromTree(char *inMsgId);

/*!
 *******************************************************************************
 * Register server attributes change callback
 *
 * \param	msgId	SCOMO message
 *
 * \return	0 on success, 1 otherwise
 *******************************************************************************
 */
VDM_Error DMA_redbend_SCOMO_registerServerAttributesChangeCallback(void);



#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_SCOMO_VSENSE_SERVER_ATTRIBUTE_CHANGE_ACTIONS_H */
