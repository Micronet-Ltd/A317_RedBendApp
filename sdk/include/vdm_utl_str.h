/*
 *******************************************************************************
 *
 * vdm_utl_str.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_utl_str.h
 *
 * \brief	ANSI String Utilities
 *******************************************************************************
 */
#ifndef VDM_UTL_STR_H
#define VDM_UTL_STR_H           //!< Internal

#ifdef __cplusplus
extern "C" {
#endif

/*!
 *******************************************************************************
 * Duplicate a string.
 *
 * Memory for the new string is acquired using \ref VDM_PL_malloc and should be
 * freed using \ref VDM_PL_free or \ref VDM_PL_freeAndNullify.
 *
 * \param	inString	The string to duplicate
 *
 * \return	A pointer to the duplicate string
 *******************************************************************************
 */
extern char *VDM_UTL_strdup(const char *inString);

/*!
 *******************************************************************************
 * Duplicate the first \a inLen characters of a string.
 *
 * Memory for the new string is acquired using \ref VDM_PL_malloc and should be
 * freed using \ref VDM_PL_free or \ref VDM_PL_freeAndNullify.
 *
 * \param	inString	The string to duplicate
 * \param	inLen		The number of characters to duplicate.
 *
 * \return	A pointer to the duplicate string
 *******************************************************************************
 */
extern char *VDM_UTL_strndup(const char *inString, IU32 inLen);

/*!
 *******************************************************************************
 * Macro for \ref VDM_UTL_stringPrintNull.
 *
 * \param	P	Buffer to print.
 *
 * \return	"[null]" if the buffer was null, otherwise the buffer is returned
 *			as entered
 *******************************************************************************
 */
#define VDM_UTL_strPrintNull(P) (VDM_UTL_stringPrintNull((const char *)(P)))

/*!
 *******************************************************************************
 * Print "[null]" instead of a null buffer.
 *
 * \param	inPtr	Buffer to print.
 *
 * \return	"[null]" if the buffer was null, otherwise the buffer is returned
 *			as entered
 *******************************************************************************
 */
extern char *VDM_UTL_stringPrintNull(const char *inPtr);

/*!
 *******************************************************************************
 * Get whether two strings are equal, ignoring case as required.
 *
 * \param	inString1	The first string
 * \param	inString2	The second string
 * \param	inIgnoreCase	TRUE to ignore case, FALSE otherwise
 *
 * \return	TRUE if the strings are equal, FALSE otherwise
 *******************************************************************************
 */
extern IBOOL VDM_UTL_strAreEqual(const char *inString1, const char *inString2,
	IBOOL inIgnoreCase);

/*!
 *******************************************************************************
 * Get the number of characters at the start of a string that are all from a
 * specified set of acceptable characters.
 *
 * \param	inString	The string
 * \param	inAccept	A string containing the specified set of
 *				acceptable characters
 *
 * \return	The length of the valid string
 *******************************************************************************
 */
IU32 VDM_UTL_strspn(const char *inString, const char *inAccept);

/*!
 *******************************************************************************
 * Get the number of characters at the start of a string that are all not from a
 * specified set of acceptable characters.
 *
 * \param	inString	The string
 * \param	inReject	A string containing the specified set of
 *				not acceptable characters
 *
 * \return	The length of the valid string
 *******************************************************************************
 */
IU32 VDM_UTL_strcspn(const char *inString, const char *inReject);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

