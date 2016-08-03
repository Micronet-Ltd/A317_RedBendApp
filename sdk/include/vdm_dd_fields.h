/*
 *******************************************************************************
 *
 * vdm_dd_fields.h
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_dd_fields.h
 *
 * \brief	OMA DM Protocol Engine download descriptor fields
 *******************************************************************************
 */

#ifdef __cplusplus
extern "C" {
#endif

#define ENUM_PREFIX E_VDM_DDField
#include "vdm_enum2string.h"

BEGIN_ENUM_PRE DECL_ENUM_ELEMENT_PRE(size)                 //!< Number of bytes to be downloaded from the URI
DECL_ENUM_ELEMENT_PRE(objectURI)                /*!< URI (usually a URL) from which the media
                                                     object can be loaded */
DECL_ENUM_ELEMENT_PRE(type)                     //!< MIME type of the media object
DECL_ENUM_ELEMENT_PRE(name)                     /*!< A user readable name of the media object
                                                     that identifies the object to the user */
DECL_ENUM_ELEMENT_PRE(DDVersion)                //!< Version of the Download Descriptor technology
DECL_ENUM_ELEMENT_PRE(vendor)                   //!< The organisation that provides the media object
DECL_ENUM_ELEMENT_PRE(description)              //!< A short textual description of the media object
DECL_ENUM_ELEMENT_PRE(installNotifyURI)         /*!< URI (or URL) to which an installation
                                                     status report is to be sent */
DECL_ENUM_ELEMENT_PRE(nextURL)                  /*!< URL to which the client should navigate in case
                                                     the user selects to invoke a browsing action
                                                     after the download transaction has completed */
DECL_ENUM_ELEMENT_PRE(infoURL)                  //!< A URL for further describing the media object
DECL_ENUM_ELEMENT_PRE(iconURI)                  //!< The URI of an icon
DECL_ENUM_ELEMENT_PRE(installParam)             /*!< An installation parameter associated with
                                                     the downloaded media object */
DECL_ENUM_ELEMENT_PRE(PreDownloadMessage)       //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(PostDownloadMessage)      //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(PostUpdateMessage)        //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(PreDownloadURL)           //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(PostDownloadURL)          //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(PostUpdateURL)            //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(RequiredInstallParameter)         //!< Verizon-specific
DECL_ENUM_ELEMENT_PRE(objectVersion)            //!< DLOTA 2.0
DECL_ENUM_ELEMENT_PRE(objectID)                 //!< DLOTA 2.0
DECL_ENUM_ELEMENT_PRE(envType)                  //!< Daimler-specific
DECL_ENUM_ELEMENT_PRE(metaUri)                  //!< Daimler-specific
END_ENUM_PRE
#undef ENUM_PREFIX

#ifdef __cplusplus
} /* extern "C" */
#endif

