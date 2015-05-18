/*
 *******************************************************************************
 *
 * vdm_swmc_pl_device.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_swmc_pl_device.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <sys/types.h>
#include <string.h>
#include <dirent.h>
#include <errno.h>
#include <jni.h>
#include <vdm_components.h>
#include <vdm_pl_types.h>
#include <vdm_error.h>
#include <vdm_pl_string.h>
#include <swm_general_errors.h>
#include <vdm_swmc_pl_device.h>
#include <vdm_utl_logger.h>
#include <vdm_utl_calloc.h>
#include <vdm_utl_utf8.h>
#include <vdm_jni_utils.h>
#include <vdm_jni_logcat.h>

#define VDM_COMPONENT_ID	(E_VDM_COMPONENT_SWMC)
#define VDM_COMPONENT_ID_E_VDM_COMPONENT_SWMC


static SWM_Error VDM_SWMC_PL_Device_getOsBuildString(const char* inField, UTF8CStr outValue, IU32* ioValueSize)
{
	SWM_Error swmResult = SWM_ERR_OK;
	VDM_Error vdmResult = VDM_ERR_OK;
	JNIEnv* env = JNU_GetEnv();
	jclass iplClass = NULL;
	jmethodID methodId = NULL;
	jstring valueObj = NULL;
	jint valueLen = 0;
	IU32 suppliedLen = 0;

	if (ioValueSize)
		VDM_logDebug(("+VDM_SWMC_PL_Device_getOsBuildStringNew. inField=%s, ioValueSize:%d",
				VDM_UTL_strPrintNull(inField), *ioValueSize));

	if(!ioValueSize)
	{
		vdmResult = VDM_ERR_BAD_INPUT;
		goto end;
	}
	suppliedLen = *ioValueSize;
	*ioValueSize = 0;

	iplClass = JNU_GetIplCls();
	if(!iplClass)
	{
		VDM_logError(("Failed to FindClass, got vdmResult 0x%x", vdmResult));
		vdmResult = VDM_ERR_UNSPECIFIC;
		goto end;
	}

	methodId = (*env)->GetStaticMethodID(env, iplClass, (const char*)inField, "()Ljava/lang/String;");
	vdmResult = JNU_handleException(env);
	if(vdmResult != VDM_ERR_OK || !methodId)
	{
		VDM_logError(("Failed to method, got vdmResult 0x%x", vdmResult));
		if (vdmResult == VDM_ERR_OK)
			vdmResult = VDM_ERR_UNSPECIFIC;
		goto end;
	}
	valueObj = (*env)->CallStaticObjectMethod ( env, iplClass, methodId, NULL ) ;
	vdmResult = JNU_handleException(env);
	if(vdmResult != VDM_ERR_OK)
	{
		VDM_logError(("Failed to CallStaticVoidMethod, got vdmResult 0x%x", vdmResult));
		goto end;
	}
	if (valueObj != NULL)
	{
		//Verify length of buffer
		valueLen = (*env)->GetStringLength(env, valueObj);
		VDM_logError(("valueLen %d", valueLen));

		if (suppliedLen < (IU32)valueLen)
		{
			vdmResult = VDM_ERR_BUFFER_OVERFLOW;
			goto end;
		}

		//get the string
		(*env)->GetStringUTFRegion(env, valueObj, 0, valueLen, (char*)outValue);
		vdmResult = JNU_handleException(env);
		if(vdmResult != VDM_ERR_OK)
		{
			VDM_logError(("Failed to GetStringUTFRegion, got vdmResult 0x%x", vdmResult));
			goto end;
		}
	}
end:
	if (valueObj)
		(*env)->DeleteLocalRef(env, valueObj);

	switch(vdmResult)
	{
		case VDM_ERR_OK:
			swmResult = SWM_ERR_OK;
			break;

		case VDM_ERR_BAD_INPUT:
			swmResult = SWM_ERR_BAD_INPUT;
			break;

		case VDM_ERR_BUFFER_OVERFLOW:
			swmResult = SWM_ERR_BUFFER_OVERFLOW;
			break;

		case VDM_ERR_MEMORY:
			swmResult = SWM_ERR_MEMORY;
			break;

		default:
			swmResult = SWM_ERR_UNSPECIFIC;
			break;
	}
	*ioValueSize = (IU32)valueLen;
	VDM_logDebug(("VDM_SWMC_PL_Device_getOsBuildString: got vdmResult 0x%x", vdmResult));
	VDM_logDebug(("-VDM_SWMC_PL_Device_getOsBuildString returns 0x%x(SWM_Error)", swmResult));
	return swmResult;
}

SWM_Error VDM_SWMC_PL_Device_getManufacturer(UTF8CStr outMan, IU32* ioManSize)
{
	SWM_Error res = VDM_SWMC_PL_Device_getOsBuildString("getManufacturer", outMan, ioManSize);
	VDM_logDebug(("VDM_SWMC_PL_Device_getManufacturer: [%s]", VDM_UTL_strPrintNull(outMan)));
	return res;
}

SWM_Error VDM_SWMC_PL_Device_getFWVersion(UTF8CStr outFWVersion, IU32* ioFWVersionSize)
{
	SWM_Error res = VDM_SWMC_PL_Device_getOsBuildString("getFwVersion", outFWVersion, ioFWVersionSize);
	VDM_logDebug(("VDM_SWMC_PL_Device_getFWVersion: [%s]", VDM_UTL_strPrintNull(outFWVersion)));
	return res;
}

SWM_Error VDM_SWMC_PL_Device_getModel(UTF8CStr outModel, IU32* ioModelSize)
{
	SWM_Error res = VDM_SWMC_PL_Device_getOsBuildString("getDevModel", outModel, ioModelSize);
	VDM_logDebug(("VDM_SWMC_PL_Device_getModelNew: [%s]", VDM_UTL_strPrintNull(outModel)));
	return res;
}

