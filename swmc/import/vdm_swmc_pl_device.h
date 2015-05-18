/*
 *******************************************************************************
 *
 * vdm_swmc_pl_device.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_swmc_pl_device.h
 *
 * \brief	Device API
 *******************************************************************************
 */

#ifndef VDM_SWMC_PL_DEVICE_H_
#define VDM_SWMC_PL_DEVICE_H_

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Get device model.
 *
 * \param	outModel		Pre-allocated buffer to store model.
 * \param	ioModelSize 	Input: Length of \a outModel.
 *							Output: Length of model, excluding null terminator.
 *
 * \return	SWM_ERR_OK on success, or an \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_Device_getModel(UTF8CStr outModel, IU32* ioModelSize);

/**
 * Get device manufacturer.
 *
 * \param	outMan		Pre-allocated buffer to store manufacturer.
 * \param	ioManSize 	Input: Length of \a outMan.
 *						Output: Length of manufacturer, excluding null
 *						terminator.
 *
 * \return	SWM_ERR_OK on success, or an \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_Device_getManufacturer(UTF8CStr outMan, IU32* ioManSize);

/**
 * Get firmware version.
 *
 * \param	outFWVersion		Pre-allocated buffer to store firmware version.
 * \param	ioFWVersionSize		Input: Length of \a outMan.
 *								Output: Length of firmware version, excluding
 *								null terminator.
 *
 * \return	SWM_ERR_OK on success, or an \ref SWM_ERR_defs error code
 */
SWM_Error VDM_SWMC_PL_Device_getFWVersion(UTF8CStr outFWVersion, IU32* ioFWVersionSize);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* VDM_SWMC_PL_DEVICE_H_ */
