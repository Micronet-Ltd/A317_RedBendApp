/*
 *******************************************************************************
 *
 * swm_dp_access.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	swm_dp_access.h
 *
 * \brief	Interface and defines for delta package handler .
 *******************************************************************************
 */

#ifndef _SWM_DP_ACCESS_H
#define _SWM_DP_ACCESS_H



#ifdef __cplusplus
extern "C" {
#endif

/**
 * None specific installer type - all installer types are supported
*/
#define ALL_INSTALLER_TYPES   0xFFFFFFFF
#define INVALID_INSTALLER_TYPE (0xFFFFFFFF - 1)

/**
 * None specific flags - all flags combinations are supported
*/
#define DP_ACCESS_FLAGS_ANY	  0xFFFFFFFF

/**
 * Critical - Only critical components are supported
*/
#define DP_ACCESS_FLAGS_CRITICAL  0x00000002

/**
 * Displayable - Only displayable components are supported
*/
#define DP_ACCESS_FLAGS_DISPLAYABLE 0x00000001

/**
 * Maximum size for component strings
*/
#define	SWM_DP_MAX_SIZE_COMP_STR		 256 + 1 // +1 for null termination

/**
 * DP Components modes
*/
typedef enum {
	DP_COMP_MODE_INSTALL,	///<  Component is installed
	DP_COMP_MODE_REMOVE,	///<  Component is removed
	DP_COMP_MODE_UPDATE,	///<  Component is updated
} DP_COMP_MODE;
 
/**
 *	Callback function. Called to indicate the component version by the component ID
 *
 *  \param  in_context Callback context
 *	\param 	in_installer_type The installer type
 *	\param 	in_comp_id The required component ID
 *	\param	out_buffer Pre-allocated buffer into which we will write the component version.
 *	\param	in_b_length The pre allocated buffer size.
 *	\param	out_v_length The actual version length
 *
 *	\return	SWM_Error								
 */
typedef SWM_Error (*get_component_version_callback_t)(void* in_context, IU32 in_installer_type,
										   UTF8Str	in_comp_id,
										   void		*out_buffer, 
										   IU32		in_b_length, 
										   IU32		*out_v_length); 

/**
 *	Returns the current DP Path
 *
 *	\return	char array representing the current DP Path								
 */
 extern char *SWM_DP_getDpPath(void);

/**
 *	Sets the current DP Path to be the given path
 *
 *	\param 	in_new_path char array representing the new DP Path	
 *
 *	\return	None								
 */
 extern void SWM_DP_setDpPath(char *in_new_path);
 
/**
 *	returns a list that contains component IDs separated by semicolons	
 *
 *	\param 	out_pp_component_ids the result char array that contains the component IDs separated by semicolons 
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_getDPComponentIDs(UTF8Str* out_pp_component_ids);

/**
 *	Returns the number of components that answer a given installer types and flags.
 *
 *	\param 	in_inst_types an array representing all supported installer types.
 *	\param 	in_n_inst_types Type the size of the installer array
 *	\param 	in_flags The supported components flags
 *	\param 	out_updates_num the number of supported components
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_getNumberOfUpdatesByInstType(
		IU32 * in_inst_types, IU32 in_n_inst_types, IU32 in_flags,
		IU32 * out_updates_num);

/**
 *	Returns the component id and update detailes for a requested component <br>
 *	described by its ordinal number in the list generated according to installer types and flags <br>
 * 	Instead of each non-required update detail - pass NULL. <br>
 *	In case only component ID is needed, use SWM_DP_getCompIdByInstType.
 *
 *	\param 	in_inst_types an array representing all supported installer types.
 *	\param 	in_n_inst_types Type the size of the installer array
 *	\param 	in_flags The supported components flags
 *	\param 	in_ordinal_num the required component ordinal number
 *	\param 	out_comp_id The component ID of the requested component (NULL if not required)
 *	\param 	out_source_ver The source version of the requested component (NULL if not required)
 *	\param 	out_target_ver The Target version of the requested component (NULL if not required)
 *	\param 	out_offset The offset of update in the DP of the requested component (NULL if not required)
 *	\param 	out_size	The size of update in the DP of the requested component (NULL if not required)
 *	\param 	out_mode The update mode of the requested component (NULL if not required)
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_getUpdateDetailsByInstType (
		IU32 * in_inst_types, IU32 in_n_inst_types, IU32 in_flags, IU32 in_ordinal_num, 
		UTF8Str out_comp_id,
		UTF8Str out_source_ver, UTF8Str out_target_ver,
		IU32* out_offset, IU32* out_size, DP_COMP_MODE * out_mode);
		
/**
 *	Returns the component id for a requested component described by its ordinal number <br>
 *	 in the list generated according to installer types and flags	 
 *
 *	\param 	in_inst_types an array representing all supported installer types.
 *	\param 	in_n_inst_types Type the size of the installer array
 *	\param 	in_flags The supported components flags
 *	\param 	in_ordinal_num the required component ordinal number
 *	\param 	out_comp_id The component ID of the requested component
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_getCompIdByInstType (
		IU32 * in_inst_types, IU32 in_n_inst_types, IU32 in_flags, IU32 in_ordinal_num, 
		UTF8Str out_comp_id);

/**
 *	Validates the DP structure.
 *
 *	\param 	get_component_version_cb Pointer to the function that enables reading the component version.
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_validate(void* cb_context, get_component_version_callback_t get_component_version_cb);

/**
 *	Checks whether the DP is marked as mandatory	
 *
 *	\param 	out_is_mandatory whether the dp is mandatory
 *
 *	\return	SWM_Error								
 */
extern SWM_Error SWM_DP_isMandatory(IBOOL * out_is_mandatory);

/**
 *	General function to get data from the Delta Package
 *
 *	\param	in_offset offset from start address of the Delta package							
 *	\param	out_buffer Pre-allocated buffer into which we will write the info read from the delta
 *	\param	in_buffer_len Length of the pre-allocated buffer
 *
 *	\return SME error code
 */
extern SWM_Error SWM_DP_readBufferFromDP(IU32 in_offset, void* out_buffer, IU32  in_buffer_len);

/**
 *	Returns the DP size, as read from the header of the DP file, including signature offset
 *
 *	\return size, in bytes, or 0 in case of an error
 */
extern IU32 SWM_DP_getDPSize(void);

/**
 * - validate external DP signature,
 * - set header offset,
 * - validates whether the DP signature matches the DP contents,
 * - get dp header parameters
 * - check dp header Preamble
 * - check dp header version
 *
 *	\return SWM error code
 */
extern SWM_Error SWM_DP_checkDP(void);

#ifdef __cplusplus
} /* extern "C" */
#endif
 

#endif
