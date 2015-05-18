/*
 *******************************************************************************
 *
 * vdm_utl_config.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 ******************************************************************************
 * \file	vdm_utl_config.h
 *
 * \brief	Configuration API
 ******************************************************************************
 */
#ifndef _VDM_UTL_CONFIG_H_
#define _VDM_UTL_CONFIG_H_

#include <vdm_pl_types.h>
#include <vdm_error.h>

#ifdef __cplusplus
extern "C"
{
#endif

#define HASH_SIZE ('z'-'0'+1)

#define CFG_FLG_INTERNAL 0x0001
#define CFG_FLG_ALLOWED_AFTER_START 0x0002

typedef enum {
	E_CFG_TYPE_str = 0,
	E_CFG_TYPE_bool = 1,
	E_CFG_TYPE_iu32 = 2,
	E_CFG_TYPE_enum = 3, // iu32 with table
	E_CFG_TYPE_file = 4 // recursive configuration file
} configType_t;

typedef struct lookupTable_t {
	char *key;
	IU32 value;
} lookupTable_t;

typedef struct VDM_UTL_Config_t VDM_UTL_Config_t;

typedef struct VDM_UTL_Config_cfg_t {
	char *group;
	char *key;
	configType_t configType;
	IU32 flags;
	char *usage;
	char *defaultValue;
	lookupTable_t *lookupTable;
	char *altKey;
	VDM_Error (*configCB)(VDM_UTL_Config_t *context,
		struct VDM_UTL_Config_cfg_t *cfg);
	union {
		char *str;
		IBOOL ibool;
		IU32 iu32;
	} value;
} VDM_UTL_Config_cfg_t;

/*!
 ******************************************************************************
 * Set a configuration parameter. Use if you don't know the type, such as when
 * parsing the configuration file.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 * \param	value		Value
 * \param	err			Output: VDM_ERR_OK on success, or a \ref VDM_Error value
 *
 * \return	TRUE on success, FALSE otherwise
 ******************************************************************************
 */
extern IBOOL VDM_UTL_Config_setValue(VDM_UTL_Config_t *inContext, char *key, char *value, VDM_Error *err);

/*!
 ******************************************************************************
 * Set a configuration string parameter.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 * \param	value		Value
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_setStr(VDM_UTL_Config_t *inContext, char *key, char *value);

/*!
 ******************************************************************************
 * Get a configuration string parameter value.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 *
 * \return	Value
 ******************************************************************************
 */
extern char *VDM_UTL_Config_getStr(VDM_UTL_Config_t *inContext, char *key);

/*!
 ******************************************************************************
 * Set a configuration boolean parameter.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 * \param	value		Value
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_setIBool(VDM_UTL_Config_t *inContext, char *key, IBOOL value);

/*!
 ******************************************************************************
 * Get a configuration boolean parameter value.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 *
 * \return	Value
 ******************************************************************************
 */
extern IBOOL VDM_UTL_Config_getIBool(VDM_UTL_Config_t *inContext, char *key);

/*!
 ******************************************************************************
 * Set a configuration integer parameter.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 * \param	value		Value
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_setIU32(VDM_UTL_Config_t *inContext, char *key, IU32 value);

/*!
 ******************************************************************************
 * Get a configuration integer parameter value.
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 *
 * \return	Value
 ******************************************************************************
 */
extern IU32 VDM_UTL_Config_getIU32(VDM_UTL_Config_t *inContext, char *key);

/*!
 ******************************************************************************
 * Set a configuration enumeration parameter value (an integer index).
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 * \param	value		Value
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_setEnum(VDM_UTL_Config_t *inContext, char *key, IU32 value);

/*!
 ******************************************************************************
 * Get a configuration enumeration parameter value (an integer index).
 *
 * \param	inContext	Configuration context
 * \param	key			Parameter name
 *
 * \return	Value
 ******************************************************************************
 */
extern IU32 VDM_UTL_Config_getEnum(VDM_UTL_Config_t *inContext, char *key);

/*!
 ******************************************************************************
 * Print usage information.
 *
 * \param	inContext			Configuration context
 * \param	inGroup				Configuration group
 * \param	inTitle				YEHUDA
 * \param	inPrintGroupPrefix	YEHUDA
 *
 * \return	None
 ******************************************************************************
 */
extern void VDM_UTL_Config_usage(VDM_UTL_Config_t *inContext, char *inGroup,
	char *inTitle, IBOOL inPrintGroupPrefix);

/*!
 ******************************************************************************
 * Dump configuration.
 *
 * \param	pContext	Configuration context
 *
 * \return	None
 ******************************************************************************
 */
extern void VDM_UTL_Config_dumpConfiguration(VDM_UTL_Config_t *pContext);

/*!
 ******************************************************************************
 * Print configuration to file.
 *
 * \param	pContext	Configuration context
 * \param	inPath		Path to file
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_printToFile(VDM_UTL_Config_t *pContext,
	char *inPath);

/*!
 ******************************************************************************
 * Initialize configuration instance.
 *
 * \param	inMutex				YEHUDA
 * \param	inIsEngineStartedCb	YEHUDA
 *
 * \return	None
 ******************************************************************************
 */
extern VDM_UTL_Config_t *VDM_UTL_Config_init(VDM_Handle_t inMutex,
	IBOOL (*inIsEngineStartedCb)(void));

/*!
 ******************************************************************************
 * Free content and terminate configuration instance.
 *
 * \param	pContext	Configuration context
 *
 * \return	None
 ******************************************************************************
 */
extern void VDM_UTL_Config_term(VDM_UTL_Config_t *pContext);

/*!
 ******************************************************************************
 * Add copnfiguration.
 *
 * \param	inContext		Configuration context
 * \param	inGroup			Configuration group
 * \param	inKey			Key
 * \param	inConfigType	Configuration type
 * \param	inFlags			Configuration flags
 * \param	inUsage			Usage information
 * \param	inDefaultValue	Default value
 * \param	inLookupTable	YEHUDA
 * \param	inAltKey		Alternative key
 * \param	inConfigCB		Callback to invoke
 *
 * \return	VDM_ERR_OK on success, or a \ref VDM_Error value
 ******************************************************************************
 */
extern VDM_Error VDM_UTL_Config_add(VDM_UTL_Config_t *inContext, char *inGroup,
	char *inKey, configType_t inConfigType, IU32 inFlags, char *inUsage, 
	char *inDefaultValue, lookupTable_t *inLookupTable, char *inAltKey,
	VDM_Error (*inConfigCB)(VDM_UTL_Config_t *context, VDM_UTL_Config_cfg_t *cfg));

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

