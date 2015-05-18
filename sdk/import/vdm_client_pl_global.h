/*
 *******************************************************************************
 *
 * vdm_client_pl_global.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_client_pl_global.h
 *
 * \brief	Platform-specific Global Pointer APIs
 *
 * vDirect Mobile requires that the application context store a global pointer
 * to a vDirect Mobile internal data structure. The Engine allocates the
 * data structure and sets the pointer when it starts up, and de-allocates and
 * resets it to NULL when it terminates. You must implement the following
 * APIs for the Engine to be able to access its data via this pointer.
 *******************************************************************************
 */

#ifndef VDM_CLIENT_PL_GLOBAL_H
#define VDM_CLIENT_PL_GLOBAL_H

#ifdef __cplusplus
extern "C" 
{
#endif

/*!
 * @defgroup pl_global	Global
 * @ingroup pl
 * @{
 */

/*!
 *******************************************************************************
 * Store data to a global pointer.
 *
 * \param	inData	A pointer to the data object created by the Engine
 * 
 * \return	TRUE on success, FALSE if the global pointer is already set or for
 * 			some other failure
 *******************************************************************************
 */
extern IBOOL VDM_Client_PL_Global_set(void* inData);

/*!
 *******************************************************************************
 * Get data from the global pointer.
 *
 * \return	A pointer to the data object created by the Engine,	or NULL if the
 * 			global pointer is not set
 *******************************************************************************
 */
extern void* VDM_Client_PL_Global_get(void);

/*!
 *******************************************************************************
 * Set the global pointer to NULL.
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_Client_PL_Global_reset(void);

/*! @} */


#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

