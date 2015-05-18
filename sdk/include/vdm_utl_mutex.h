/*
 *******************************************************************************
 *
 * vdm_utl_mutex.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_utl_mutex.h
 *
 * \brief	Platform-agnostic Re-entrant (recursive) Mutex API
 *
 * A mutex is a synchronization object that is used by threads to control access
 * to a shared resource. A mutex can be locked to indicate a resource is in use.
 * Other threads can then block on the mutex to wait for the resource.
 *
 * "Mutex" is short for "mutual exclusion".
 *******************************************************************************
 */
#ifndef VDM_UTL_MUTEX_H
#define VDM_UTL_MUTEX_H			//!< Internal

#ifdef __cplusplus
extern "C" {
#endif

#define VDM_UTL_Mutex_INVALIDREFCOUNT		-1		//!< Invalid number of references

typedef struct VDM_UTL_Mutex_t VDM_UTL_Mutex_t;

/*!
 *******************************************************************************
 * Create and open a mutex.
 *
 * \note	If you leave this implementation empty, do not return NULL, as this
 *			is interpreted as an error.
 *
 * \return	Handle to the mutex
 *******************************************************************************
 */
VDM_UTL_Mutex_t *VDM_UTL_Mutex_create(void);

/*!
 *******************************************************************************
 * Lock a mutex.
 *
 * \param	inMutex		Handle to the mutex
 *
 * \return	Reference count or
 * 			VDM_UTL_Mutex_INVALIDREFCOUNT if \a inMutex is NULL
 *******************************************************************************
 */
IS16	VDM_UTL_Mutex_lock(VDM_UTL_Mutex_t *inMutex);

/*!
 *******************************************************************************
 * Unlock a mutex.
 *
 * \param	inMutex		Handle to the mutex
 *
 * \return	Reference count or
 * 			VDM_UTL_Mutex_INVALIDREFCOUNT if \a inMutex is NULL, you tried to
 * 			unlock the mutex from a non-owning thread, or you tried to unlock an
 * 			unlocked mutex
 *******************************************************************************
 */
IS16	VDM_UTL_Mutex_unlock(VDM_UTL_Mutex_t *inMutex);

/*!
 *******************************************************************************
 * Close a mutex.
 *
 * \param	inMutex		Handle to the mutex
 *
 * \return	None
 *******************************************************************************
 */
void	VDM_UTL_Mutex_close(VDM_UTL_Mutex_t *inMutex);


#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

