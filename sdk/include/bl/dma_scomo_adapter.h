/*
 *******************************************************************************
 *
 * dma_scomo_adapter.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
********************************************************************************
*  \file dma_scomo_adapter.h
*
*  \brief vDM SCOMO-SWM Adapter - this is a glue layer between BL layer and
*  installers layer
*
*******************************************************************************
*/

#ifndef _DMA_SCOMO_ADAPTER_H_
#define _DMA_SCOMO_ADAPTER_H_

#ifdef __cplusplus
extern "C" 
{
#endif

#include <vdm_pl_types.h>
#include <dma_scomo_bl_types.h>

/*!
 *******************************************************************************
 * Create and initialize the SCOMO SWM adapter layer
 *
 * \param	inAppContext	application context
 * \param	inFilesDir		application working directory
 * \param	inFileInstallerFilePath installer working directory
 *
 * \return	TRUE on success, or a FALSE on error
 *******************************************************************************
 */
IBOOL DMA_redbend_ScomoSwmAdapter_init(void *inAppContext,
    const char *inFilesDir, const char *inFileInstallerFilePath);

VDM_Error DMA_redbend_ScomoSwmAdapter_isScomoInstallNeedReboot(char *inPkgPath,
	IBOOL *outIsNeedReboot);
	
/*!
 *******************************************************************************
 * Destroy the SCOMO SWM adapter layer
 *
 *******************************************************************************
 */
void DMA_redbend_ScomoSwmAdapter_destroy(void);

/*!
 *******************************************************************************
 * Get next component
 *
 *
 * \param	id			Component ID
 * \param	ioIdLen		Component ID length
 *
 * \return	1 if has more components , or 0 on last component or error
 *******************************************************************************
 */
int DMA_redbend_ScomoSwmAdapter_getNextComp(char *id, int *ioIdLen);

/*!
 *******************************************************************************
 * Call installers layers - for both update and get results
 *
 *
 * \param	inDpPath		DP directory path
 * \param	inDpFileName    DP file path
 * \param	inDpHandle		DP context
 * \param	isAfterReboot	TRUE if execute was called after reboot (get results)
 * 							FALSE if execute was called before reboot (run installer/start update)
 * \param	inOperationType	operation type
 *
 *******************************************************************************
 */
void DMA_redbend_ScomoSwmAdapter_execute(const char *inDpPath,
	const char *inDpFileName, VDM_Handle_t inDpHandle, int inIsAfterReboot,
	E_OperationType_t inOperationType);

/*!
 *******************************************************************************
 * Get firmware version string. In RB solution (BL) this function is registered
 * external to the node:  ./DevDetail/FwV.
 *
 * \param	inContext	context
 * \param	inOffset	offset in buffer
 * \param	outBuffer	Allocated buffer to hold the fw version string. SWM server
 * 						expects the string to be NON-null terminated.
 * \param	inBufSize	size of allocated buffer
 * \param	outDataLen	length of FW version string. SWM server expects the string
 * 						length to be NON-null terminated.
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_ERR_defs error code
 *******************************************************************************
 */
VDM_Error DMA_redbend_ScomoSwmAdapter_readFirmwareVersion(void *inContext,
		IU32 inOffset, void *outBuffer, IU32 inBufSize, IU32 *outDataLen);

char *DMA_redbend_ScomoSwmAdapter_getDpPath(void);

IU32 DMA_redbend_ScomoSwmAdapter_getDpSize(void);

void DMA_redbend_ScomoSwmAdapter_setInstallResult(int inResult);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /*_DMA_SCOMO_ADAPTER_H_*/

