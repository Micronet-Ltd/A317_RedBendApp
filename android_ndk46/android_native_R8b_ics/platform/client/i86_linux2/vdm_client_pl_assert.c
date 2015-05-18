/*
 *******************************************************************************
 *
 * vdm_client_pl_assert.c
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file vdm_client_pl_assert.c
 *
 * \brief vDirect Mobile SDK
 *******************************************************************************
 */

#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>

#include <vdm_pl_types.h>
#include "vdm_client_pl_linux_logger.h"
#include <vdm_client_pl_assert.h>

#ifndef PROD

void VDM_Client_PL_assertFail(const char *inFile, IS32 inLine)
{
	char msgBuf[100] = {0};
	int i;

	/* trim file path to the last 20 characters */
	char *ptr = (char*)(inFile + strlen(inFile) - 20);

	snprintf(msgBuf, sizeof(msgBuf), "%s\tline: %ld\t file:%s\n", VDM_ASSERT_PREFIX, inLine, ptr);
	for (i = 0; i < 3; i++)
	{
    	VDM_Client_PL_Linux_LogDebug_output(0, msgBuf);
        printf("%s", msgBuf);
    }
}

#endif	//PROD

