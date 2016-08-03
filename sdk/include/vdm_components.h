/*
 *******************************************************************************
 *
 * vdm_components.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_components.h
 *
 * \brief	Enumeration of all OMA DM Protocol Engine SDK Components
 *
 * When adding a new component to OMA DM Protocol Engine, add an enum value.
 *******************************************************************************
 */

#ifndef VDM_COMPONENTS_H
#define VDM_COMPONENTS_H

#ifdef __cplusplus
extern "C" {
#endif

/*!
 * OMA DM Protocol Engine components
 */

#include "vdm_enum2string.h"

/*!
 * Component Definitions.
 *
 * \defgroup VDM_componentType Component Definitions
 * @{
 */
BEGIN_ENUM(E_VDM_COMPONENT_TYPE)
	//vDM Framework components (must be first)
    DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_CLIENT_PL, "Client_PL")   ///< Client porting layer
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_UTIL, "Util")				///< Generic utilities
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_MMI, "MMI")				///< MMI
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_COMM, "Comm")				///< Communication
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_HTTP, "HTTP")				///< HTTP Library package
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_SMM, "SMM")				///< State Machine Manager
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_IPC, "IPC")				///< SMM IPC
	//vDM Core Engine components
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_CORE, "Core_MsgQ")		///< Core component
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_DEVICE, "Device")			///< Device specific
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM, "Core_Eng")			///< RDM - General	(was TRACE)
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_TRG, "Core_NIA")		///< RDM Trigger (RDM=>DMC) NIA/Bootstrap
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_SESS, "Core_Sess")	///< RDM Session Manager
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_SESSQ, "Core_SessQ")	///< RDM Session Queue
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_COMM, "Core_Comm")	///< RDM Session comm	(was: IO)
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_SYNCML, "SyncML") 	///< (was: RTK & XPT)
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_TREE, "Core_Tree")	///< RDM Tree
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_AUTH, "Core_Auth")	///< RDM Authentication
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_WBXML, "Core_WBXML")	///< RDM WBXML
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_RDM_DL, "Core_DL")		///< RDM Download

	//Packages, extensions and MO components
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_CLIENT, "DMC")			///< The DM client application
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_FUMO, "FUMO")				///< FUMO component
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_SCOMO, "SCOMO")			///< SCOMO component
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_BOOTSTRAP, "Bootstrap")	///< Bootstrap extension
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_CONNMO, "ConnMO")    		///< ConnMO component
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_WSP, "WSP")				///< WSP Library package
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_LAWMO, "LAWMO")			///< LAWMO component
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_DL, "ADL")				///< DL extension
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_SWMC, "SWMC")				///< SWM Client
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_DESCMO, "DESCMO")			///< Dev Settings Configuration MO
	DECL_ENUM_ELEMENT_STR(E_VDM_COMPONENT_XML, "XML")				///< XML package

	// Add new components here.  <<============
	DECL_ENUM_ELEMENT(E_VDM_COMPONENT_RDM_TEMP)						///< Internal
	DECL_ENUM_ELEMENT(E_VDM_COMPONENT_COUNT)						///< Must be last
END_ENUM(E_VDM_COMPONENT_TYPE)

/** @} */

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif //VDM_COMPONENTS_H

