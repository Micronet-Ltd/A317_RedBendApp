/*
 *******************************************************************************
 *
 * vdm_utl_dynbuffer.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_utl_dynbuffer.h
 *
 * \brief	Dynamic buffer utility
 *******************************************************************************
 */


#ifndef VDM_UTL_DYNBUFFER_H_ 	//!< Internal.
#define VDM_UTL_DYNBUFFER_H_

#ifdef __cplusplus
extern "C" {
#endif


/*!
 * Dynamic buffer
 */
typedef struct
{
	IU32	initialSize;//!< "Private" member - for internal use
	IU32 	maxCapacity;//!< "Private" member - for internal use
	IU32	capacity;	//!< "Private" member - for internal use
	IU8*	workspace;	//!< Data buffer
	IU32	length;		//!< Current data length ( <= capacity )

}VDM_UTL_DynBuffer_t;


/*!
 *******************************************************************************
 * Create a dynamic buffer.
 *
 * \param	inInitialSize	Initial size of buffer. "0" indicates 32 KB.
 * \param	inMaxSize		Maximum size of buffer. "0" indicates MAX_IU32.
 * \param	outResult		VDM_ERR_OK on success,
 * 							VDM_ERR_BAD_INPUT if \a inMaxSize <
 * 							\a inInitialSize, or VDM_ERR_MEMORY if there is no
 * 							memory to perform the action.
 *
 * \return	Pointer to the dynamic buffer, a \ref VDM_UTL_DynBuffer_t structure
 *******************************************************************************
 */
extern VDM_UTL_DynBuffer_t* VDM_UTL_DynBuffer_create(
	IU32 		inInitialSize,
	IU32 		inMaxSize,
	VDM_Error	*outResult);

/*!
 *******************************************************************************
 * Destroy a dynamic buffer. Free any memory allocated for the buffer.
 *
 * \param	ioBuffer	In: Pointer to the dynamic buffer.
 *						Out: NULL.
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_UTL_DynBuffer_destroy(VDM_UTL_DynBuffer_t** ioBuffer);

/*!
 *******************************************************************************
 * Append data to the end of a dynamic buffer.
 *
 * If the data exceeds the buffer length, the buffer is resized as required,
 * up to the maximum set in \ref VDM_UTL_DynBuffer_create.
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 * \param	inData		Data.
 * \param	inDataLen	Length of \a inData.
 *
 * \return	VDM_ERR_OK on success,
 * 			VDM_ERR_MEMORY if there is no memory to perform the action, or
 * 			VDM_ERR_BUFFER_OVERFLOW if adding the data will exceed maximum
 * 			buffer size
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_DynBuffer_append(
	VDM_UTL_DynBuffer_t*	inBuffer,
	IU8*					inData,
	IU32					inDataLen);

/*!
 *******************************************************************************
 * Set data at a specific offset in a dynamic buffer.
 *
 * If \a inOffset + \a inDataLen exceeds current buffer length, the buffer is
 * resized as required, up to the maximum set in \ref VDM_UTL_DynBuffer_create.
 *
 * \note	Existing data at the offset is overwritten!
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 * \param	inOffset	Offset within buffer to begin.
 * \param	inData		Data.
 * \param	inDataLen	Length of \a inData.
 *
 * \return	VDM_ERR_OK on success,
 * 			VDM_ERR_BAD_INPUT if inBuffer is NULL,
 * 			VDM_ERR_MEMORY if there is no memory to perform the action, or
 * 			VDM_ERR_BUFFER_OVERFLOW if adding the data will exceed maximum
 * 			buffer size
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_DynBuffer_setAt(
	VDM_UTL_DynBuffer_t*	inBuffer,
	IU32					inOffset,
	IU8*					inData,
	IU32					inDataLen);

/*!
 *******************************************************************************
 * Reset dynamic buffer to its initial size and zero out buffer (reset all data
 * to \\0).
 *
 * \note	Resetting the buffer to its original size frees and reallocates
 *			memory, so there is a slight chance of failure on low memory.
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 *
 * \return	VDM_ERR_OK on success or
 * 			VDM_ERR_MEMORY if there is no memory to perform the action.
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_DynBuffer_reset(VDM_UTL_DynBuffer_t* inBuffer);

/*!
 *******************************************************************************
 * Zero out (replace with \\0's) part of a dynamic buffer. No memory is
 * reallocated.
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 * \param	inStartPos	Offset within buffer to begin.
 * \param	inLength	Number of bytes to set to zero.
 *
 * \return	VDM_ERR_OK on success,
 * 			VDM_ERR_BAD_INPUT if inBuffer is NULL,
 * 			VDM_ERR_BUFFER_OVERFLOW if \a startPos exceeds the capacity.
 *******************************************************************************
 */
extern VDM_Error VDM_UTL_DynBuffer_bzero(
	VDM_UTL_DynBuffer_t* 	inBuffer,
	IU32					inStartPos,
	IU32					inLength);

/*!
 *******************************************************************************
 * Set the length of the dymnamic buffer without modifying the data.
 *
 * If the new length exceeds current buffer length, the buffer is resized as
 * required, up to the maximum set in \ref VDM_UTL_DynBuffer_create.
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 * \param	inLength	New length.
 *
 * \return	VDM_ERR_OK on success,
 * 			VDM_ERR_MEMORY if there is no memory to perform the action, or
 * 			VDM_ERR_BUFFER_OVERFLOW if new length will exceed maximum
 * 			buffer size.
 *******************************************************************************
 */
VDM_Error VDM_UTL_DynBuffer_setLength(
	VDM_UTL_DynBuffer_t* 	inBuffer,
	IU32					inLength);

/*!
 *******************************************************************************
 * Get part of a dynamic buffer.
 *
 * \note	If \a inStartPos or \a inEndPos exceed the buffer capacity, only the
 *			range up until the capacity is dumped.
 *
 * \param	inBuffer	Pointer to the dynamic buffer, a
 *						\ref VDM_UTL_DynBuffer_t structure
 * \param	inStartPos	Start position.
 * \param	inEndPos	End position.
 * \param	inMsg		Optional text to return before buffer contents.
 *
 * \return	None.
 *******************************************************************************
 */
void VDM_UTL_DynBuffer_dump(
	VDM_UTL_DynBuffer_t* 	inBuffer,
	IU32					inStartPos,
	IU32					inEndPos,
	const char				*inMsg);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif // VDM_UTL_DYNBUFFER_H_
