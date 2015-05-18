/*
 *******************************************************************************
 *
 * vdm_pl_alloc.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_pl_alloc.h
 *
 * \brief	Memory Allocation API
 *******************************************************************************
 */

#ifndef _VDM_PL_ALLOC_H_
#define _VDM_PL_ALLOC_H_				//!< Internal

#ifdef __cplusplus
extern "C" {
#endif

#include <vdm_pl_types.h>

/*!
 * @addtogroup pl_system
 * @{
 */

/*!
 * Macro to free a non-NULL pointer and set it to NULL.
 */
#define VDM_PL_zeroFreeAndNullify(ioPtr, inLen)	\
	do {								\
		if ((ioPtr))			\
		{								\
			VDM_PL_memset((ioPtr), 0, (inLen));	\
			VDM_PL_free((ioPtr));		\
			(ioPtr) = NULL;				\
		}								\
	} while (0)

/*!
 * Macro to free a non-NULL pointer and set it to NULL.
 */
#define VDM_PL_zeroFreeAndNullify(ioPtr, inLen)	\
	do {								\
		if ((ioPtr))			\
		{								\
			VDM_PL_memset((ioPtr), 0, (inLen));	\
			VDM_PL_free((ioPtr));		\
			(ioPtr) = NULL;				\
		}								\
	} while (0)


/*!
 * Macro to free a non-NULL pointer and set it to NULL.
 */
#define VDM_PL_freeAndNullify(ioPtr)	\
	do {								\
		if ((ioPtr))					\
		{								\
			VDM_PL_free((void*)(ioPtr));\
			(ioPtr) = NULL;				\
		}								\
	} while (0)

/*!
 *******************************************************************************
 * Allocate a memory block.
 *
 * \note	The size to allocate may not be a multiple of 4.
 *
 * \see		VDM_UTL_calloc
 *
 * \param	inSize		Number of bytes to allocate
 *
 * \return	Void pointer to the allocated space,
 *			or NULL if there is insufficient memory available
 *******************************************************************************
 */
extern void* VDM_PL_malloc(IU32 inSize);


/*!
 *******************************************************************************
 * Free a memory block. This function frees a pointer without checking if it is
 * NULL or setting the pointer to NULL.
 *
 * \note	It is strongly recommended to use the safer macro
 * 			\ref VDM_PL_freeAndNullify, rather than this function.
 *          This macro first checks that the pointer is not already NULL and sets
 *          the pointer to NULL after the memory is de-allocated.
 *
 * \param	inPtr	Pointer to a previously allocated memory block to be freed
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_PL_free(void *inPtr);

/*! @} */

#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

