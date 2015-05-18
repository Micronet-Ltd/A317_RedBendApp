/*
 *******************************************************************************
 *
 * swm.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	swm.h
 *
 * \brief	Software Management Client
 *******************************************************************************
 */
#ifndef _SWM_H_ 
#define _SWM_H_

#ifdef __cplusplus
extern "C" {
#endif

#define EXTERNAL_UA_COMP_ID "External UA firmware"

#define SCOUT_OP_STR ((UTF8Str)"SCOUT")
#define UPDATE_OP_STR ((UTF8Str)"UPDATE")
#define UPDATE_MULTI_OP_STR ((UTF8Str)"MULTI-UPDATE")
#define UPDATE_MULTI_END_OP_STR ((UTF8Str)"MULTI-UPDATE-END")
#define SET_STATE_OP_STR ((UTF8Str)"SET_STATE")
#define HANDLE_INVALID_OP_STR ((UTF8Str)"HANDLE_INVALID")

/**
 * DP installation results
 */
typedef enum {
	SWM_COMMAND_RESULT_ERR		= -1,	///< Fatal error
	SWM_COMMAND_RESULT_OK		= 0,	///< Installed successfully
	SWM_COMMAND_RESULT_REBOOT	= 1,	/**< Reboot required to complete
											installation */

} SWM_command_result_t;

/**
 * Component installation results
 */
typedef enum {
	COMP_RES_OK	= 0,			///< Success
	COMP_RES_ERR	= 1,		///< Failure
	COMP_RES_NOT_ATTEMPTED = 2,	///< Not attempted due to previous error
} comp_result_t;

/**
 * Operations that can be performed on an installed component
 */
typedef enum {
	SWM_COMP_OP_ACTIVATE	= 1,	///< Activate
	SWM_COMP_OP_DEACTIVATE	= 2,	///< Deactivate
	SWM_COMP_OP_REMOVE		= 3,	///< Remove
} SWM_comp_op_t;

/**
 * SWM client object
 */
typedef struct SWM_t SWM_t;

/**
 * Installation plan.
 *
 * A list of instructions (with optional component types) that order the DP
 * installation.
 * 
 * See example in main.cpp.
 */
typedef struct {
	UTF8Str cmd;	///< Instruction
	void * param;	///< Component type, when applicable
} SWM_plan_t;

/**
 * Parameters to pass when installing a DP.
 */
typedef struct {
	UTF8Str path;				///< Path for temporary data (in UTF-8)
	SWM_plan_t *plan;			/**< Installation plans (NULL-terminated array of
									\ref SWM_plan_t values) */
	void *asyncHandle;			/**< Handle for asynchronous operations; passed
									to SWM_PL_async() */
	IBOOL isUnifiedProgress;	/**< Whether progress is measured for all
								  	 components together or separately by
									 components */
} SWM_params_t;

/**
 * Callback structures to pass when installing a DP. 
 */
typedef struct {
/**
 * Callback invoked to report installation progress.
 */
	IS32 (*update_progress_cb) (void *inUser, IU32 inPercent, IU32 inType);
/**
 * Callback invoked to notify when the installation process initiated by
 * \ref SWM_Client_Run is completed.
 */
	IS32 (*update_completed)(void *inUser, SWM_command_result_t inRes);
} SWM_callbacks_t;

/**
 * Component attributes.
 *
 * Retrieved by \ref SWM_Client_getComponentAttribute.
 */
typedef enum {
	SWM_COMP_ATTR_NAME,	///< Component name
	SWM_COMP_ATTR_DESC,	///< Component description
	SWM_COMP_ATTR_VER,	///< Component version
	SWM_COMP_ATTR_TYPE,	///< Component type
} SWM_component_attr;

/**
 * Callback invoked to create installer.
 *
 * \param	inTypes			List of component types supported by the installer.
 * \param	inTypesCount	Number of component types supported by the installer.
 * \param	inParams		Optional parameters.
 *
 * \return	Handle to the installer
 */
typedef void * (*SWMC_InstallerCreate_CB_t)(IU32 *inTypes, IU32 inTypesCount, void *inParams);

/**
 * Callback invoked to destroy installer.
 *
 * \param	ioInstaller	Handle to the installer.
 *
 * \return	None
 */
typedef void (*SWMC_InstallerDestroy_CB_t)(void *ioInstaller);

/**
 * Register new installer.
 *
 * \param	inSwm			Handle to the SWM client instance.
 * \param	inInitFunc		Create installer function.
 * \param	inDestroyFunc	Destroy installer function.
 * \param	inTypes			List of component types supported by the installer.
 * \param	inTypesCount	Number of component types supported by the installer.
 * \param	inParams		Optional parameters required for the create function.
 *
 * \return	SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_registerInstaller(SWM_t *inSwm,
								SWMC_InstallerCreate_CB_t inInitFunc,
								SWMC_InstallerDestroy_CB_t inDestroyFunc,
								IU32 *inTypes, IU32 inTypesCount, void *inParams);

/**
 * Register new external UA installer (to be used when the DP cannot be
 * authenticated).
 * It will also be registered as a regular installer with
 * \ref SWM_Client_registerInstaller.
 *
 * \param	inSwm			Handle to the SWM client instance.
 * \param	inInitFunc		Create installer function.
 * \param	inDestroyFunc	Destroy installer function.
 * \param	inTypes			List of component types supported by the installer.
 * \param	inTypesCount	Number of component types supported by the installer.
 * \param	inParams		Optional parameters required for the create function.
 *
 * \return	SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_registerExternalUaInstaller(SWM_t *inSwm,
								SWMC_InstallerCreate_CB_t inInitFunc,
								SWMC_InstallerDestroy_CB_t inDestroyFunc,
								IU32 *inTypes, IU32 inTypesCount, void *inParams);

/**
 * Create SWM client instance.
 *
 * \return	Handle to the SWM client instance
 */
SWM_t *SWM_Client_create(void);

/**
 * Install DP.
 *
 * \param	inSwm				SWM instance.
 * \param	inParams			Additional parameters, an \ref SWM_params_t value.
 * \param	inCbs				Callback structure to handle installation events, an
 *								\ref SWM_callbacks_t value.
 * \param	inUser				Context passed to callback functions.
 * \param	inIsResume			Whether to resume after reboot or power down. Always
 *								used for firmware component.
 *
 * \return	SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_run(SWM_t *inSwm, SWM_params_t *inParams, SWM_callbacks_t *inCbs,
						void *inUser, IBOOL inIsResume);

/**
 * Destroy SWM client instance.
 *
 * \param	in_swm		SWM instance.
 *
 * \return	None
 */
void SWM_Client_destroy(SWM_t *inSwm);

/**
 * Get next component id.
 *
 * This function iterates over the all components in the device's inventory.
 *
 * \param	inSWM		SWM instance.
 * \param	ioIt		Input: iterator pointer, NULL to get first component id.
 *						Output: iterator pointer, NULL indicates end of list.
 * \param	outId		Pre-allocated buffer into which to write the component
 *						id.
 * \param	ioIdSize	Size of \a outId buffer.
 *
 * \return	SWM_ERR_OK on success or an SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_getNextComponent(SWM_t *inSWM, void **ioIt, UTF8Str outId, IU32 *ioIdSize);

/**
 * Perform an operation on a component.
 *
 * \param	inSwm		SWM instance.
 * \param	inCompId	Component id.
 * \param	inOp		Operation to perform, an \ref SWM_comp_op_t value.
 *
 * \return	0 on success or an SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_componentOperation(SWM_t *inSwm, UTF8Str inCompId, SWM_comp_op_t inOp);

/**
 * Get component attribute.
 *
 * \param	inSWM			SWM instance.
 * \param	inId			Component id, a NULL-terminated string value.
 * \param	inAttr			Component attribute, an \ref SWM_component_attr
 *							value.
 * \param	outBuffer		Pre-allocated buffer into which to write the
 *							attribute data.
 * \param	inBufferSize	Size of \a out_buffer.
 *
 * \return	0 on success or an SWM error code, an \ref SWM_Error value
 */

SWM_Error SWM_Client_getComponentAttribute(SWM_t *inSWM, UTF8CStr inId, SWM_component_attr inAttr,
										   UTF8Str outBuffer, IU32 inBufferSize);
/**
 * Get next installation result.
 *
 * This function iterates over the installation results of all components in the
 * last installed DP.
 *
 * \param	inSwm			SWM instance.
 * \param	ioItr			Input: iterator pointer, NULL to get first component id.
 *							Output: iterator pointer, NULL indicates end of list.
 * \param	outCompId		Pre allocated buffer into which to write the component
 *							id.
 * \param	outRes			Pre-allocated buffer into which to write the component
 *							installation result, an \ref comp_result_t value.
 * \param	outMode			Pre-allocated buffer into which to write the component
 *							mode, a \ref DP_COMP_MODE value.
 *
 * \return	0 on success or an SWM error code, an \ref SWM_Error value
 */
SWM_Error SWM_Client_getNextResult(SWM_t *inSwm, void **ioItr, UTF8Str outCompId, comp_result_t *outRes, DP_COMP_MODE *outMode);

/**
 * Install callback, used by Android installer
 */
typedef SWM_Error (*SWM_InstallCb)(char *inComponent);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif

