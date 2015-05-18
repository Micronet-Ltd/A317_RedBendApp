/*
 *******************************************************************************
 *
 * vdm_ipc.h
 *
 * Copyright (c) 2005-2014, Red Bend Software. All Rights Reserved.
 *
 *******************************************************************************
 */

/*!
 *******************************************************************************
 * \file	vdm_ipc.h
 *
 * \brief	Inter Process Communication
 *
 * The SMM Engine runs first and waits for the DIL to connect using IPC.
 * SMM Engine and DIL are peers, client and server to each other.
 *
 * Android doesn't use IPC functions, but uses the package and parse functions
 * (\ref VDM_IPC_genEvent and \ref VDM_IPC_parseEvent).
 *******************************************************************************
 */

#ifndef _VDM_IPC_H_
#define _VDM_IPC_H_

#ifdef __cplusplus
extern "C" {
#endif


#include <vdm_smm_types.h>

#define VDM_IPC_MSG_SIZE (sizeof(uint32_t))

/* fs list is kept as a sparsed int array, first non-zero element is used,
 * when removing fd, it is set to zero, and will be reused next time a new
 * element is added */
#define VDM_IPC_INSTANCE_MAX_FDS 8

typedef struct {
	int serverfd;
	int stopPipefd[2];
	int fds[VDM_IPC_INSTANCE_MAX_FDS];
} VDM_IPC_Instance_t;

/**
 * Waits for a server destined traffic to be available
 *
 * \param	ipc				The handler to the IPC instance
 *
 * \return	non-zero on success
 */
int VDM_IPC_serverSelect(VDM_IPC_Instance_t *ipc);

/**
 * Waits for a client destined traffic to be available
 *
 * \param	sockfd			The socket id
 *
 * \return	negative in case of an error, zero on success
 */
int VDM_IPC_clientSelect(int sockfd);

/**
 * Get an event from the server IPC channel.
 *
 * \param	ipc				The handler to the IPC instance
 * \param	event			The event
 * \param	non_blocking	Non-zero if a blocking socket, 0 otherwise
 *
 * \return	0 on success, 1 on a parser error, or an error message
 */
int VDM_IPC_serverReadEvent(VDM_IPC_Instance_t *ipc, VDM_SMM_Event_t **event,
	int non_blocking);

/**
 * Sends a "wake-up" byte to pipe that's included in the select waiting for
 * incoming events, will cause the blocking read to interrupt.
 * Returns zero on success, -errno on error case
 *
 * \param	ipc				The handler to the IPC instance
 *
 * \return	0 on success, or an negative number signing an error
 */
int VDM_IPC_serverStopRead(VDM_IPC_Instance_t *ipc);

/**
 * Get an UI event from client IPC channel.
 *
 * \param	sockfd			The socket id
 * \param	event			The event
 * \param	non_blocking	Non-zero if a blocking socket, 0 otherwise
 *
 * \return	0 on success, 1 on a parser error, or an error message
 */
int VDM_IPC_clientReadUiEvent(int sockfd, VDM_SMM_Event_t **event, int non_blocking);

/**
 * Send UI event over server IPC channel.
 *
 * \param	ipc			The handler to the IPC instance
 * \param	event		The event
 *
 * \return	1 on success, 0 if the transmitted event buffer allocation failed, 
 * or an error number
 */
int VDM_IPC_serverPostUiEvent(VDM_IPC_Instance_t *ipc, VDM_SMM_Event_t *event);

/**
 * Send a client event over IPC channel.
 *
 * \param	sockfd		The socket id
 * \param	event		The event
 *
 * \return	1 on success, 0 if the transmitted event buffer allocation failed,
 * or an error number
 */
int VDM_IPC_clientPostEvent(int sockfd, VDM_SMM_Event_t *event);

/**
 * Parse received event from IPC channel.
 *
 * An event is packaged before sending by \ref ipc_getEvent.
 *
 * \param	readArray		A packaged event
 *
 * \return	The unpackaged event
 */
VDM_SMM_Event_t *VDM_IPC_parseEvent(unsigned char *readArray);

/**
 * Package an event for sending over IPC channel.
 *
 * The event is unpackaged on the other side by \ref ipc_parseEvent.
 *
 * \param	event		The event
 * \param	writeArray	The packaged event
 * \param	arraySize	Size of \a writeArray
 *
 * \return	The size of the package; if the size is greater than \a arraySize,
 *			this is the required size, and the packaging was unsuccessful 
 */
IU32 VDM_IPC_genEvent(VDM_SMM_Event_t *event, unsigned char *writeArray,
	IU32 arraySize);

/**
 * Start TCP IPC server.
 *
 * \param	addr			The IPC server address
 * \param	port			The IPC server port
 *
 * \return	The handler to the IPC instance or NULL in the case of an error
 */
VDM_IPC_Instance_t *VDM_IPC_initTcpServer(const char *addr, int port);

/**
 * Start UNIX IPC server.
 *
 * \param	file			The file socket the server is bind to
 *
 * \return	The handler to the IPC instance or NULL in the case of an error
 */
VDM_IPC_Instance_t *VDM_IPC_initUnixServer(const char *file);

/**
 * Returns the number of connected clients over server IPC channel.
 *
 * \param	ipc				The handler to the IPC instance
 *
 * \return	Number of clients connected
 */
int VDM_IPC_getNumberOfClients(VDM_IPC_Instance_t *ipc);

/**
 * Terminate IPC server.
 *
 * \param	ipc				The handler to the IPC instance
 *
 * \return	None
 */
void VDM_IPC_termServer(VDM_IPC_Instance_t *ipc);

/**
 * Start TCP IPC client.
 *
 * \param	addr			The IPC server address
 * \param	port			The IPC server port
 *
 * \return	The socket id, or an error number
 */
int VDM_IPC_initTcpClient(const char *addr, int port);

/**
 * Start UNIX IPC client.
 *
 * \param	file			The file socket client will connect
 *
 * \return	The socket id, or an error number
 */
int VDM_IPC_initUnixClient(const char *file);

/**
 * Terminate IPC client.
 *
 * \param	sockfd		The socket to the server
 *
 * \return	None
 */
void VDM_IPC_termClient(int sockfd);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* _VDM_IPC_H_ */

