/*
 *******************************************************************************
 *
 * vdm_client_pl_storage.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_client_pl_storage.h
 *
 * \brief	Persistent Storage APIs
 *
 * OMA DM Protocol Engine does not assume that the device supports a file system. The
 * storage APIs support opening and closing a storage item, reading and
 * writing to and from an open storage item, and deleting a storage item from
 * persistent storage.
 *
 * In operating systems that use a file system, a storage item is typically
 * a file.
 *******************************************************************************
 */
#ifndef VDM_CLIENT_PL_STORAGE_H
#define VDM_CLIENT_PL_STORAGE_H

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * @defgroup pl_storage	Storage
 * @ingroup pl
 * @{
 */

#define VDM_Client_PL_Filename_dlresume	"dlresume.dat"

/*!
 * Access type for storage items
 *
 * A storage item can be opened in read-only or write modes. If a non-existent
 * storage item is opened in write mode, the function must create the item.
 */
typedef enum 
{
	E_VDM_CLIENT_PL_Storage_Access_read,	/**< Read mode */
	E_VDM_CLIENT_PL_Storage_Access_write,	/**< Write mode, if files exists erase it */
	E_VDM_CLIENT_PL_Storage_Access_append	/**< Append mode, from start ("a+") */
} E_VDM_CLIENT_PL_Storage_Access_t;

/*!
 * OMA DM Protocol Engine defines the following types of storage items that it uses when
 * calling the storage Porting Layer APIs.
 */
typedef enum 
{
	E_VDM_CLIENT_PL_StorageItem_DMTree,		/**< DM Tree */
	E_VDM_CLIENT_PL_StorageItem_Config,		/**< Configuration file (not used by vDM)*/
	E_VDM_CLIENT_PL_StorageItem_DLResume,	/**< Download resume data */
	E_VDM_CLIENT_PL_StorageItem_Registry	/** Settings and options for the
											    various Framework components */
} E_VDM_CLIENT_PL_StorageItem_t;

/*!
 *******************************************************************************
 * Allocate memory for a storage item.
 *
 * \note	Free the memory using \ref VDM_PL_free or \ref VDM_PL_freeAndNullify
 *
 * \param	inType		Type of storage item, an
 *						\ref E_VDM_CLIENT_PL_StorageItem_t value
 *
 * \return	Pointer to allocated buffer, or NULL on failure
 *******************************************************************************
 */
extern UTF8CStr VDM_Client_PL_Storage_getPathDup(E_VDM_CLIENT_PL_StorageItem_t inType);

/*!
 *******************************************************************************
 * Load a file to a buffer.
 *
 * The function opens the file, reads and loads it to the preallocated buffer,
 * and then closes it.
 *
 * \param	inFileName	    The file name
 * \param	outBuffer	    The preallocated buffer to hold the file's content
 * \param   inBufSize       The size of \a outBuffer
 * \param   outReadCount    The number of bytes stored in \a outBuffer
 * \param   outFileSize     The file size in bytes
 *
 * \return	VDM_ERR_OK on success, VDM_ERR_BAD_INPUT,
 *          VDM_ERR_STORAGE_OPEN if there is a problem opening the file,
 *          VDM_ERR_STORAGE_READ if there is a problem reading the file, or
 *          VDM_ERR_BUFFER_OVERFLOW if the supplied buffer was too small to hold
 *          the file contents
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_loadFile(char *inFileName,
	void *outBuffer, IU32 inBufSize, IU32 *outReadCount, IU32 *outFileSize);

/*!
 *******************************************************************************
 * Open (stream) a storage item by specifying its type.
 *
 * Use this function to open any item with a \ref E_VDM_CLIENT_PL_StorageItem_t
 * value. For other items, use \ref VDM_Client_PL_Storage_openByName.
 *
 * A storage item can be opened in read-only or write modes. If a non-existent
 * storage item is opened in write mode, this function must create the item.
 *
 * \param	outHandle	Handle to the open storage item
 * \param	inType		Type of storage item, an
 *						\ref E_VDM_CLIENT_PL_StorageItem_t value
 * \param	inMode		Type of access required, an
 * 						\ref E_VDM_CLIENT_PL_Storage_Access_t value
 *
 * \return	VDM_ERR_OK on success, VDM_ERR_MEMORY if out of memory, or
 * 			VDM_ERR_STORAGE_OPEN if there is a problem opening the item
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_open(void **outHandle,
	E_VDM_CLIENT_PL_StorageItem_t inType, E_VDM_CLIENT_PL_Storage_Access_t inMode);
	
/*!
 *******************************************************************************
 * Open (stream) a storage item by specifying its name.
 *
 * A storage item can be opened in read-only or write modes. If a non-existent
 * storage item is opened in write mode, this function must create the item.
 *
 * \note	Do not use this function to open any file with a
 *			\ref E_VDM_CLIENT_PL_StorageItem_t value. Instead, use
 *			\ref VDM_Client_PL_Storage_open.
 *
 * \param	outHandle	Handle to the open storage item
 * \param	inFileName	The name of the file
 * \param	inMode		Type of access required, an
 * 						\ref E_VDM_CLIENT_PL_Storage_Access_t value
 *
 * \return	VDM_ERR_OK on success, VDM_ERR_MEMORY if out of memory, or
 * 			VDM_ERR_STORAGE_OPEN if there is a problem opening the item
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_openByName(void **outHandle,
	const char *inFileName, E_VDM_CLIENT_PL_Storage_Access_t inMode);

/*!
 *******************************************************************************
 * Read a line from an open storage item.
 *
 * Read a line from the current position and append it, with a terminating null,
 * to \a outBuffer. After the read operation is finished, the file pointer
 * must be positioned at the end of the data read.
 *
 * \param	inHandle		Handle to the open storage item
 * \param	outBuffer		Pre-allocated buffer to store the string. A maximum
 *							of ( \a inBufferSize - 1 ) characters are read,
 *							stored in the buffer, and appended with a
 *							terminating null
 * \param	inBufferSize    The size of \a outBuffer
 *
 * \return	The string, or
 *          NULL if an error occurred or if there was nothing to read
 *******************************************************************************
 */
extern UTF8Str VDM_Client_PL_Storage_fgets(void *inHandle, void *outBuffer,
	IU32 inBufferSize);

/*!
 *******************************************************************************
 * Read data from storage.
 *
 * After the read operation is finished, the file pointer must be positioned
 * at the end of the data read.
 *
 * \param	inHandle		Handle to the storage item
 * \param	outBuffer		Buffer to store the read data 
 * \param	inBufSize		The size of \a outBuffer, in bytes
 * \param	outReadCount	Number of bytes actually read<br>
 *							This will be \a inBufSize unless there is
 *							no more data, in which case the value must be 0.
 * 
 * \return	VDM_ERR_OK on success, VDM_ERR_MEMORY if out of
 * 			memory, or VDM_ERR_STORAGE_READ if there is a problem reading
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_read(void *inHandle, void *outBuffer,
	IU32 inBufSize, IU32 *outReadCount);

/*!
 *******************************************************************************
 * Write data to storage.
 *
 * After the write operation is finished, the file pointer must be
 * positioned at the end of the data written.
 *
 * \param	inHandle		Handle to the storage item
 * \param	inData			Data to write.
 * \param	inLength		The length of \a inData, in bytes
 * 
 * \return	VDM_ERR_OK on success, VDM_ERR_MEMORY if out of
 * 			memory, or VDM_ERR_STORAGE_WRITE if there is a problem writing
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_write(void *inHandle, const void *inData,
	IU32 inLength);

/*!
 *******************************************************************************
 * Close a storage item.
 *
 * \param	inHandle		Handle to the storage item
 * \param	inCommit		TRUE if written data should be committed;
 *							FALSE if storage was opened for reading
 * 
 * \return	VDM_ERR_OK on success, VDM_ERR_MEMORY if out of memory,
 * 			or VDM_ERR_STORAGE_COMMIT if there is a problem committing
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_close(void *inHandle, IBOOL inCommit);

/*!
 *******************************************************************************
 * Delete a storage item, given its type.
 *
 * \param	inType		Type of storage item, an
 *						\ref E_VDM_CLIENT_PL_StorageItem_t value
 *
 * \return	None
 *******************************************************************************
 */
extern void VDM_Client_PL_Storage_delete(E_VDM_CLIENT_PL_StorageItem_t inType);

/*!
 *******************************************************************************
 * Delete a storage item, given its file name.
 *
 * \param	inFileName	The name of the file to delete.
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_STORAGE_REMOVE otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_deleteByName(const char *inFileName);

/*!
 * File relative seek starting point
 */
typedef enum {
	E_VDM_CLIENT_PL_FSeek_START,	///< Seek from the beginning of the file
	E_VDM_CLIENT_PL_FSeek_CURRENT,	///< Seek from the current file pointer
	E_VDM_CLIENT_PL_FSeek_END,		///< Seek back from the end of the file
} E_VDM_CLIENT_PL_FSeek_t;

/*!
 *******************************************************************************
 * Seek to an offset in a file relative to a starting point:
 * - Forward from the start of the file
 * - Forward from the current file pointer
 * - Backwards from the end of the file
 *
 * After the operation is finished, the file pointer must be positioned at the
 * offset on success, the beginning of the file if searching backwards, or the
 * end of the file if searching forward.
 *
 * \param	inHandle	Handle to the storage item
 * \param	inOffset	Offset to seek
 * \param	inSeekType 	Seek starting point, an \ref E_VDM_CLIENT_PL_FSeek_t
 *						value
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_STORAGE_SEEK otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_fileSeek(void *inHandle, IU32 inOffset,
	E_VDM_CLIENT_PL_FSeek_t inSeekType);

/*!
 *******************************************************************************
 * Sync a storage item with external storage (write all changes to file).
 *
 * \param	inHandle	Handle to the storage item
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_UNSPECIFIC otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_sync(void *inHandle);

/*!
 * VDM storage porting layer stat structure
 */
typedef struct VDM_Client_PL_Storage_stat_t {
	IBOOL isExist; /**< File exist*/
	IBOOL isDirectory; /**< File is a directory */
	IBOOL isExecutable; /**< File is executable */
	IBOOL isWritable; /**< File is writable */
	IBOOL isReadable; /**< File is readable */
	IU32  availableSpace; /**< disk free space */
	IS32  size; /**< File is readable */
} VDM_Client_PL_Storage_stat_t;

/*!
 *******************************************************************************
 * Retrieve file statistics
 *
 * \param	inFileName	Full path of file
 * \param	outStat		statistics struct (VDM_Client_PL_Storage_stat_t) 
 *
 * \return	VDM_ERR_OK even if file does not exist
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_statByName(char *inFileName,
	VDM_Client_PL_Storage_stat_t *outStat);

/*!
 * VDM storage porting layer permissions flags.
 */
#define VDM_PL_STORAGE_ADD_EXEC 0x1 /**< Add execute permission */
#define VDM_PL_STORAGE_ADD_WRITE 0x2 /**< Add write permission */
#define VDM_PL_STORAGE_ADD_READ 0x4 /**< Add read permission */
#define VDM_PL_STORAGE_REMOVE_EXEC 0x8 /**< Remove execute permission */
#define VDM_PL_STORAGE_REMOVE_WRITE 0x10 /**< Remove write permission */
#define VDM_PL_STORAGE_REMOVE_READ 0x20 /**< Remove read permission */

/*!
 *******************************************************************************
 * Set permissions
 *
 * \param	inFileName	Full path of file
 * \param	inFlags bitwise combination of VDM_PL_STORAGE_XXX flags
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_STORAGE_WRITE/VDM_ERR_STORAGE_READ
 * 	otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_setPermissions(char *inFileName,
	IU32 inFlags);

/*!
 *******************************************************************************
 * Check if a file exists.
 *
 * \param	inFileName	File name to find
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_STORAGE_READ otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_isFileExists(char *inFileName);

/*!
 *******************************************************************************
 * isFileExists
 *
 * \param	inDirName	Directory name to create
 *
 * \return	VDM_ERR_OK on success, or VDM_ERR_STORAGE_WRITE otherwise
 *******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_createDir(char *inDirName);

/*!
*******************************************************************************
 * Get the extracted file size information, without extracting the file.
 *
 * \return	The future uncompressed file size, (outSize)
*******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_getExtractFileSize(char *inFileName, IU32 *outSize);

/*!
*******************************************************************************
 * Extract a file.
 * Notice: file postfix can be .zip/.tar, etc'...
 *
 * \return	VDM_ERR_OK upon a successful call,
 *			or VDM_ERR_UNSPECIFIC if the file couldn't be extract for any reason.
*******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_extractFile(char *inFileName, char *inNewpath);

/*!
*******************************************************************************
 * Remove dir file.
 * Removing dir/files in recursive mode using rm -rf dommand*
 * \return	VDM_ERR_OK upon a successful call,
 *
*******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_removeDir(char *inFileName);

/*!
*******************************************************************************
 * Compress one or more files and directories into an archive.
 *
 * \param	inArchivePath	Path of archive file.
 * \param	inArchiveName	Archive file name.
 * \param	Var args		list of file names (char*) to add to the archive
 * 							(could be dir names as well).
 *							Last param in args must be NULL.
 *
 * \return	VDM_ERR_OK on success.
 *
*******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_compress(const char *inArchivePath,
	const char *inArchiveName, ...);

/*!
*******************************************************************************
 * Copy file from source to destination.
 *
 * \param	inSrc	Path of file to copy.
 * \param	inDst	Destination path.
 *
 * \return	VDM_ERR_OK on success.
 *
*******************************************************************************
 */
extern VDM_Error VDM_Client_PL_Storage_copyFile(char *inSrc, char *inDst);

/*! @} */

#ifdef __cplusplus
} /* extern "C" */
#endif


#endif

