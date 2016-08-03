/*
 *******************************************************************************
 *
 * vdm_swmc_ecu_prot_pl.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_swmc_ecu_prot_pl.h
 *
 * \brief	ECU IPL Transmitter APIs
 *******************************************************************************
 */
#ifndef _ECU_PROT_PL_H_
#define _ECU_PROT_PL_H_

#ifdef __cplusplus
extern "C" {
#endif

/** The action to perform on the ECU or the response that is expected */
typedef enum
{
// Get current ECU version number
	E_SWM_PROT_Tag_getVersion			= 0x01,
// Install a component. This invokes installer type
// IT_ECU_NONE_REDBEND (102).
	E_SWM_PROT_Tag_install			= 0x02,
// Update a component. This invokes installer type IT_ECU_REDBEND (100).
	E_SWM_PROT_Tag_update				= 0x03,
// Perform a FUSE update. This invokes installer type IT_ECU_FUSE (101).
	E_SWM_PROT_Tag_fuseUpdate			= 0x04,
// Install a component using an external generic installer, without the
// update agent. This invokes installer type IT_ECU_EXTERNAL (103).
	E_SWM_PROT_Tag_externalInstall 	= 0x05,
// Remove a component. Currently, this resets the component to its
// baseline release version.
	E_SWM_PROT_Tag_remove				= 0x06,
// Get the ECU source image path.
	E_SWM_PROT_Tag_sourceDataPath		= 0x07,
// Response only: get the percent progress of an operation.
	E_SWM_PROT_Tag_progress			= 0x08,
// Response only: get the result of an operation.
	E_SWM_PROT_Tag_updateResult		= 0x09,
// Get next ECU component ID: This request is intended to iterate over
// the IDs. For each request, return the next ID. Return NULL to indicate
// the end of the list of ECU components. This ID is not related to the
// ECU hardware ID below.
	E_SWM_PROT_Tag_getCompId			= 0X0A,
// Ask the ECU to release the structure sent with the previous request.
	E_SWM_PROT_Tag_releaseMemory		= 0X0B,
// Get the ECU hardware serial number.
	E_SWM_PROT_Tag_getSerialNumber	= 0x0C,
// Get the ECU hardware part number.
	E_SWM_PROT_Tag_getPartNumber		= 0x0D,
// Get the ECU hardware ID of the ECU’s management unit. If the
// ECU does not have a management unit, this must return null.
	E_SWM_PROT_Tag_getManagementUnit	= 0x0E,
// Get the ECU hardware address.
	E_SWM_PROT_Tag_getNodeAddress		= 0x0F,
// Ask the vehicle to begin collecting ECU data. Collecting hardware data
// (getSerialNumber, getPartNumber, getManagementUnit, getNodeAddress,
// and getEcuId) from the ECUs might take a long time (several minutes).
// To collect ECU data, the engine first sends this init request. The
// engine then sends the other requests for hardware data from each ECU.
	E_SWM_PROT_Tag_init				= 0x10,
// Not yet implemented
	E_SWM_PROT_Tag_term				= 0x11,
// Get the next ECU hardware ID: This request is intended to iterate over
// the IDs. For each request, return the next ID. Return NULL to indicate
// the end of the list of ECUs. The ECU hardware ID is either the node
// address (the location on the CAN bus) or any other string that can
// uniquely identify the ECU. This ID is not related to the ECU component
// ID above.
	E_SWM_PROT_Tag_getEcuId			= 0x12,
// Get current ECU name
	E_SWM_PROT_Tag_getName			= 0x13,
} E_SWM_PROT_Tag_t;


/** String data structure for union */
typedef struct
{
	unsigned int 			length;
	const unsigned char 	*string;
} SWM_PROT_TLV_Str_t;

/** Bin data structure for union */
typedef struct
{
	int			size;
	void		*data;
} SWM_PROT_TLV_Data_t;

/** Signed integer data structure for union */
typedef struct
{
	int		number;
} SWM_PROT_TLV_Number_t;

/**
 * Values for the structure union. For an operation like install or update, the
 * request includes the DP to install or update as a data in this union. The
 * response is typically a string or number in this union. */
typedef enum {
	E_SWM_PROT_DataType_num ,
	E_SWM_PROT_DataType_bin ,
	E_SWM_PROT_DataType_str ,
} E_SWM_PROT_DataType_t;

/**
 * Installation phase -- Update of ECU can be done in few phases which are informed
 * with this enum
 */
typedef enum {
    E_SWM_INS_PHASE_PRE_INSTALL = 0,
    E_SWM_INS_PHASE_INSTALL,
    E_SWM_INS_PHASE_POST_INSTALL,
} E_SWM_PROT_InsPhase_t;


/** Request / response structure */
typedef struct
{
	E_SWM_PROT_Tag_t			tag;
	SWM_PROT_TLV_Str_t		  	comp_id;
    E_SWM_PROT_InsPhase_t       ins_phase;
    IU32                        ins_type;
	E_SWM_PROT_DataType_t		data_type;
union {
		SWM_PROT_TLV_Str_t   	 str;
		SWM_PROT_TLV_Data_t      bin;
		SWM_PROT_TLV_Number_t	 num;
	  } data; 
}  SWM_PROT_TLV_Command_t;

/**
 * Initialize ECU handler.
 *
 * \param   inContext      Context to pass to other methods
 *
 * \return	VDM_ERR_OK for success or VDM_ERR_FAIL for failure
 */
extern VDM_Error SWMC_PL_ECU_init(void **inContext);

/**
 * Send request package. 
 *
 * \param	inContext       Context
 * \param	inCmd			Command, an \ref SWM_PROT_TLV_Command_t value
 *
 * \return	VDM_ERR_OK for success or VDM_ERR_FAIL for failure
 */
extern VDM_Error SWMC_PL_ECU_request(void *inContext, SWM_PROT_TLV_Command_t *inCmd);

/**
 * Request a response package. The data union in the structure must be filled
 * by the ECU.
 *
 * \param	inContext       Context
 * \param	outCmd			Command, an \ref SWM_PROT_TLV_Command_t value
 *
 * \return	VDM_ERR_OK for success or VDM_ERR_FAIL for failure. Note that
 *			this return value only indicates if the command executed; the
 *			actual response is in \a outCmd.
 */
extern VDM_Error SWMC_PL_ECU_response(void *inContext, SWM_PROT_TLV_Command_t *outCmd);

/**
 * Terminate ECU handler.
 *
 * \param	inContext       Context
 */
extern void SWMC_PL_ECU_term(void *inContext);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif //_ECU_PROT_PL_H_
