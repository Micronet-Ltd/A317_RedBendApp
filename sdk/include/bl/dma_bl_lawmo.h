/*
 *******************************************************************************
 *
 * dma_bl_lawmo.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file dma_bl_lawmo.h
 *
 * \brief LAWMO business logic - manages calls to VDM API mainly to LAWMO
 *******************************************************************************
 */

#ifndef _DMA_REDBEND_BL_LAWMO_H_
#define _DMA_REDBEND_BL_LAWMO_H_

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Initialize and allocates LAWMO business logic before VDM engine is started
 *
 * \return	0 on success, else an error
 *******************************************************************************
 */
int DMA_redbend_lawmo_init(void);

/*!
 *******************************************************************************
 * Initialize LAWMO business logic after VDM engine already started
 *
 * \param	inRoot	Path to LAWMO instance root node in the DM tree.
 *
 * \return	0 on success, else an error
 *******************************************************************************
 */
int DMA_redbend_lawmo_postInit(UTF8CStr inRoot);

/*!
 *******************************************************************************
 * Free allocations of LAWMO business logic
 *
 *******************************************************************************
 */
void DMA_redbend_lawmo_unInit(void);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _DMA_REDBEND_BL_LAWMO_H_ */
