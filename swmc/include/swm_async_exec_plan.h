/*
 *******************************************************************************
 *
 * swm_async_exec_plan.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	swm_async_exec_plan.h
 *
 * \brief	Interface for execution of swm plan
 * 			step by step, in an asynchronous  manner.
 *******************************************************************************
 */

#ifndef SWM_ASYNC_EXEC_PLAN_H_
#define SWM_ASYNC_EXEC_PLAN_H_

#ifdef __cplusplus
extern "C" {
#endif

SWM_Error SWM_Async_execPlan(SWM_t *inSwm, SWM_params_t *inParams,
		SWM_callbacks_t *inCbs, void *inUser, IBOOL inIsResume);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* SWM_ASYNC_EXEC_PLAN_H_ */
