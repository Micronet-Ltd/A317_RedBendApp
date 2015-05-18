/*
 *******************************************************************************
 *
 * vdm_utl_persistentdata.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file    vdm_utl_persistentdata.h
 *
 * \brief   Persistent Storage API
 *******************************************************************************
 */


#ifndef _VDM_UTL_PERSISTENTDATA_H_
#define _VDM_UTL_PERSISTENTDATA_H_

#ifdef __cplusplus
extern "C"
{
#endif

/*!
 * Persistent storage data types. Used when invoking the compatability
 * callbacks.
 *
 * When you invoke the callbacks, you invoke all callbacks registered for
 * a specific type of data. For example, all callbacks to update the DM Tree. 
 */
typedef enum
{
    E_VDM_PERSISTENT_UTL_DMTree = 0,    /**< DM Tree */
    E_VDM_PERSISTENT_UTL_Registry,      /**< Configuration file (not used by vDM)*/
    E_VDM_PERSISTENT_UTL_Last = E_VDM_PERSISTENT_UTL_DMTree     /**< The type of data that will be updated last. After this type of data is updated, the storage is flashed. */
} E_VDM_PERSISTENT_UTL_ItemType_t;

#define VERSION_ROOT "SOFTWARE\\RedBend"

/*!
 *******************************************************************************
 * Initialize persistent data.
 *
 * \param   outDataHandle   Handle to data read from a path within persistent
 *                          storage
 * \param   inRoot          The path within persistent storage where the data
 *                          is stored
 * \param   inKey           The key<br>
 *                          The key is concatenated to \a inRoot to create the
 *                          full path to the storage item.
 *
 * \return  VDM_ERR_OK on success,
 *          VDM_ERR_MEMORY if no memory to perform action, or
 *          VDM_ERR_MO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_init(VDM_Handle_t *outDataHandle, char *inRoot,
    const char *inKey);

/*!
 *******************************************************************************
 * Commit data to the persistent storage.
 *
 * \param   inDataHandle     Handle to the persistent data object
 *
 * \return  VDM_ERR_OK on success, or
 *      VDM_ERR_MO_STORAGE if error creating persistent storage file
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_commit(VDM_Handle_t ioDataHandle);

/// @cond EXCLUDE
/*
 *******************************************************************************
 * Internal. Do not use.
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_globalCommit(void);

/// @endcond

/*!
 *******************************************************************************
 * Close the handle to the persistent data object, and optionally commit the
 * data to persistent storage.
 *
 * \param   i0DataHandle    Input: Handle to the persistent data object;
 *                          Output: NULL
 * \param   inCommit        True to commit changes to persistent storage, False
 *                          otherwise
 *
 * \return None
 *******************************************************************************
 */
void VDM_UTL_PersistentData_term(VDM_Handle_t *ioDataHandle, IBOOL inCommit);

/*!
 *******************************************************************************
 * Delete a handler that was created with \ref VDM_UTL_PersistentData_init. This
 * deletes the handler and all of its sub-keys from persistent storage.
 *
 * \param	inDataHandle	 Handle to the data object
 *
 * \return	VDM_ERR_OK on success, or
 *			VDM_ERR_MO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_deleteKey(VDM_Handle_t inDataHandle);

/*!
 *******************************************************************************
 * Write string to persistent data object.
 *
 * \param   inDataHandle    Handle to the persistent data object
 * \param   inKey           The key
 * \param   inStringValue   The string to store
 *
 * \return  VDM_ERR_OK on success, or
 *          VDM_ERR_FUMO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_writeString(VDM_Handle_t inDataHandle,
    const char *inKey, const char *inStringValue);

/*!
 *******************************************************************************
 * Read string from persistent data object.
 *
 * \param   inDataHandle        Handle to the persistent data object
 * \param   inKey               The key
 * \param   outValueBuffer      Pre-allocated buffer to store the string
 * \param   ioLengthValueBuffer Input: \a outValueBuffer length;
 *                              Output: Length of result string
 *
 * \return  VDM_ERR_OK on success, 
 *          VDM_ERR_MO_STORAGE if error accessing persistent storage
 *          VDM_ERR_NODE_MISSING if key was not found
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_readString(VDM_Handle_t inDataHandle,
    const char *inKey, char *outValueBuffer, IU32 *ioLengthValueBuffer);

/*!
 *******************************************************************************
 * Write an integer to persistent data object.
 *
 * \param   inDataHandle    Handle to the persistent data object
 * \param   inKey           The key
 * \param   inIntValue      The integer to store
 *
 * \return  VDM_ERR_OK on success, or
 *          VDM_ERR_MO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_writeInt(VDM_Handle_t inDataHandle,
    const char *inKey, IU32 inIntValue);

/*!
 *******************************************************************************
 * Read an integer from persistent data object.
 *
 * \param   inDataHandle    Handle to the persistent data object
 * \param   inKey           The key
 * \param   outIntValue     The stored integer value
 *
 * \return  VDM_ERR_OK on success, or
 *          VDM_ERR_MO_STORAGE if error accessing persistent storage
 *          VDM_ERR_NODE_MISSING if key was not found
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_readInt(VDM_Handle_t inDataHandle,
    const char *inKey, IU32 *outIntValue);

/*!
 *******************************************************************************
 * Write binary data to persistent data object.
 *
 * \param   inDataHandle    Handle to the persistent data object
 * \param   inKey           The key
 * \param   inBinValue      The binary data to store
 * \param   inLen           inBinValue length
 *
 * \return  VDM_ERR_OK on success, or
 *          VDM_ERR_FUMO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_writeBin(VDM_Handle_t inDataHandle,
    const char *inKey, IU8 *inBinValue, IU32 inLen);

/*!
 *******************************************************************************
 * Read binary data from persistent data object.
 *
 * \param   inDataHandle        Handle to the persistent data object
 * \param   inKey               The key
 * \param   outValueBuffer      Pre-allocated buffer to store the binary data
 * \param   ioLengthValueBuffer Input: \a outValueBuffer length;
 *                              Output: Length of result binary data
 *
 * \return  VDM_ERR_OK on success, 
 *          VDM_ERR_MO_STORAGE if error accessing persistent storage
 *          VDM_ERR_NODE_MISSING if key was not found
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_readBin(VDM_Handle_t inDataHandle,
    const char *inKey, IU8 *outValueBuffer, IU32 *ioLengthValueBuffer);

/**
 *******************************************************************************
 * Delete an item from persistent storage.
 *
 * \param	inDataHandle	Handle to the data in persistent storage
 * \param	inKey			The key of the item to delete
 *
 * \return	VDM_ERR_OK on success,
 *			VDM_ERR_MO_STORAGE if error accessing persistent storage
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_deleteItem(VDM_Handle_t inDataHandle,
    const char *inKey);

/// @cond EXCLUDE

/*
 *******************************************************************************
 * Destroy all persistent data objects. Internal.
 *******************************************************************************
 */
void VDM_UTL_PersistentData_destroy(void);

/// @endcond

/*!
 *******************************************************************************
 * Set the current version of vDirect Mobile. Call this at startup, after
 * registering the callback using \ref VDM_UTL_PersistentData_compatRegister and
 * before calling \ref VDM_UTL_PersistentData_init.
 *
 * \param   inCurrentVersion        Current vDirect Mobile version.
 *******************************************************************************
*/
void VDM_UTL_PersistentData_compatInit(char *inCurrentVersion);


/*!
 *******************************************************************************
 * Callback invoked to convert the old persistent storage items to the new
 * format, as required. Each will be invoked only once, the first time that the
 * new version of vDirect Mobile is run. Each callback is associated with a
 * specific type of data.
 *
 * \param	inOldVersion		The old vDirect Mobile version
 * \param	inCurrentVersion	The new vDirect Mobile version
 * \param	inType				The data type, an
 *								\ref E_VDM_PERSISTENT_UTL_ItemType_t value
 *******************************************************************************
 */
typedef void (*VDM_UTL_PersistentData_compatCodeCB_t)(const char *inOldVersion,
	const char *inCurrentVersion, E_VDM_PERSISTENT_UTL_ItemType_t inType);

#define COMMIT_PS "commit-ps"

/*!
 *******************************************************************************
 * Register the above callback. You can register multiple callbacks for
 * different versions.
 *
 * \param   inOlderThanVersion      Version number. The callback will be invoked
 *                                  if the old version of vDirect Mobile is this
 *                                  number or earlier.
 *                                  Pass COMMIT_PS to commit to persistent
 *                                  storage (last callback)
 * \param   inCb                    Callback to invoke.
 *
 * \return  VDM_ERR_OK on success,
 *          VDM_ERR_MEMORY on memory allocation errors,
 *          VDM_ERR_INVALID_CALL on invalid arguments
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_compatRegister(const char *inOlderThanVersion,
    VDM_UTL_PersistentData_compatCodeCB_t inCb);

/*!
 *******************************************************************************
 * Invoke all callbacks registered for the specified type of data. Called in
 * \ref VDM_create (for registry data) and at the end of \ref VDM_start (for DM
 * Tree data).
 *
 *
 *
 * \return  VDM_ERR_OK on success,
 *          VDM_ERR_MEMORY on memory allocation errors,
 *          VDM_ERR_BAD_INPUT_on invalid arguments
 *******************************************************************************
 */
VDM_Error VDM_UTL_PersistentData_runCb(E_VDM_PERSISTENT_UTL_ItemType_t inType);

#ifdef __cplusplus
} /* extern "C" */
#endif


#endif  //_VDM_UTL_PERSISTENTDATA_H_
