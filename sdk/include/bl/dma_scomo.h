/*
 *******************************************************************************
 *
 * dma_scomo.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_scomo.h
 *
 * \brief SCOMO business logic - manages calls to VDM API mainly to SCOMO/FUMO.
 * The BL manages both DL and INS and reporting to the server.
 *******************************************************************************
 */

#ifndef _DMA_SCOMO_H_
#define _DMA_SCOMO_H_

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Initialize and allocates SCOMO business logic before VDM engine is started
 *
 * \return	0 on success, else an error
 *******************************************************************************
 */
int DMA_redbend_scomo_swm_init(void *inAppContext, const char *inFilesDir);

/*!
 *******************************************************************************
 * Initialize SCOMO business logic after VDM engine already started
 *
 * \param	inScomoRoot		Path to SCOMO instance root node in the DM tree.
 * \param	inFumoRoot		Path to FUMO instance root node in the DM tree.
 * \param	inConfigFile	Configuration file name
 *
 * \return	0 on success, else an error
 *******************************************************************************
 */
VDM_Error DMA_redbend_scomo_swm_postInit(UTF8CStr inScomoRoot, UTF8CStr inFumoRoot,
	char *inConfigFile);

/*!
 *******************************************************************************
 * Free and destroy allocations of SCOMO business logic
 *
 *******************************************************************************
 */
void DMA_redbend_scomo_swm_unInit(void);

int DMA_redbend_scomo_swm_postponesm_init();
int DMA_scomo_swm_vsense_server_attribute_change_init();

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_SCOMO_H_ */
