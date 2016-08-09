/*
 *******************************************************************************
 *
 * NetClient.java
 *
 * Handles network communications (TCP/IP). Not used by the provided
 * implementation of the SWM DM Client.
 *
 * Copyright (c) 1999-2015 Red Bend. All Rights Reserved.
 *
 *******************************************************************************
 */

package com.redbend.app;

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import android.os.PowerManager;
import android.util.Log;

public abstract class NetClient 
{
	private final static String LOG_TAG = "NetClient";
	private final static int TIME_BETWEEN_SOCKET_RETRIES = 1000; // in ms
	private String addr;
	private int port;
	private byte[] evBuffer;
	private DataInputStream evData;
	private Socket socket;
	private Thread readThread, writeThread;
	private LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>();
	private Object connectedSignal = new Object();
	private boolean running;
	private boolean isWriteInterrupted;
	private final PowerManager.WakeLock mWl;

	private class SocketReader implements Runnable {
		private DataInputStream in;
		private boolean firstRun = true;

		public void run() {
			while (running) {
				Boolean acquiredLock = mWl == null;

				synchronized (connectedSignal) {
					if (firstRun) {
						/* make sure this thread has started, before the writing thread, so
						 * notify the launcher thread that "we" are ready */
						synchronized (this) {
							notify();
							firstRun = false;
						}
					}
					while (true) {
						try {
							connectedSignal.wait();
							in = new DataInputStream(socket.getInputStream());
							break;
						} catch (InterruptedException e) {
							// if interrupted, continue waiting
						} catch (IOException e) {
							// in case of an error, the world will crash...
							e.printStackTrace();
							return;
						}
					}
				}

				while (true) {
					Event ev = null;

					try {
						if (!acquiredLock && in.available() > 0) {
							mWl.acquire();
							Log.d(LOG_TAG, "There's data, holding WakeLock");
							acquiredLock = true;
						}

						ev = getEvent(in);
					} catch (IOException e) {
						e.printStackTrace();
						// let's hope we could still receive events
						continue;
					}

					// ev is null when the connection was closed
					if (ev == null) {
						if (acquiredLock && mWl != null)
							mWl.release();
						break;
					}

					if (mWl != null) {
						boolean moreData;

						if (!acquiredLock) {
							mWl.acquire();
							acquiredLock = true;
						}
						Log.d(LOG_TAG, "Received event with WakeLock: " + ev.getName());
						receiveEvent(ev);

						try {
							moreData = in.available() > 0;
						} catch (IOException e) {
							moreData = false;
						}
						if (moreData)
							Log.d(LOG_TAG, "There's more data, holding WakeLock");
						else {
							mWl.release();
							Log.d(LOG_TAG, "Released WakeLock for received event: " + ev.getName());
							acquiredLock = false;
						}
					}
					else {
						Log.d(LOG_TAG, "Received event: " + ev.getName());
						receiveEvent(ev);
					}
						
				}
				Log.e(LOG_TAG, "READ: Error in communication");
				// interrupt the write thread, so it retries connection
				if (running && !isWriteInterrupted)
					writeThread.interrupt();
			}
			Log.i(LOG_TAG, "Read thread terminated.");
		}
	}

	private class SocketWriter implements Runnable {
		private DataOutputStream out;

		public void run() {
			boolean errorDisplayed = false;

			while (running) {
				synchronized (connectedSignal) {
					while (true) {
						try {
							socket = new Socket(InetAddress.getByName(addr), port);
							socket.setKeepAlive(true);
							out = new DataOutputStream(socket.getOutputStream());
							Log.i(LOG_TAG, "Connected to IPC from port " + socket.getLocalPort());
							isWriteInterrupted = false;
							// notify all of the socket connection
							connectedSignal.notify();
							break;
						} catch (UnknownHostException e) {
							Log.e(LOG_TAG, "Cannot connect to host, host unknown");
							return;
						} catch (SocketException e) {
							if (!errorDisplayed)
							{
								Log.w(LOG_TAG, "Cannot configure socket: " + e.getMessage() + ", retrying");
								errorDisplayed = true;
							}
							try {
								connectedSignal.wait(TIME_BETWEEN_SOCKET_RETRIES);
							} catch (InterruptedException e1) {
								// if interrupted, then just continue
							}
						} catch (IOException e) {
							Log.e(LOG_TAG, "Cannot connect to host");
						}
					}
				}

				while (true)
				{
					try {
						byte event[];
						Event ev = eventQueue.take();

						Log.d(LOG_TAG, "sending event " + ev.getName());
						event = ev.toByteArray();
						out.writeInt(event.length);
						out.write(event);
						out.flush();
					} catch (InterruptedException e) {
						Log.w(LOG_TAG, "Write interrupted, stopped sending events");
						break;
					} catch (IOException e) {
						Log.w(LOG_TAG, "Write failed, retrying connection");
						break;
					}
				}
				isWriteInterrupted = true;
				errorDisplayed = false;
				try {
					socket.shutdownInput();
					socket.shutdownOutput();
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Log.i(LOG_TAG, "Write thread terminated.");
		}
	}

	public NetClient(String addr, int port, int initialBufferSize)
	{
		this(null, addr, port, initialBufferSize);
	}
	
	public NetClient(PowerManager.WakeLock wakeLock, String addr, int port, int initialBufferSize)
	{
		this.addr = addr;
		this.port = port;
		
		evBuffer = new byte[initialBufferSize];
		evData = new DataInputStream(new ByteArrayInputStream(evBuffer));
		Log.d(LOG_TAG, "Initial event buffer size: " + initialBufferSize);

		mWl = wakeLock;
	}

	public void startThreads() {
		SocketReader reader = new SocketReader();

		readThread = new Thread(reader);
		writeThread = new Thread(new SocketWriter());

		running = true;
		synchronized (reader) {
			readThread.start();
			for (;;) {
				try {
					reader.wait();
				} catch (InterruptedException e) {
					continue;
				}
				break;
			}
			writeThread.start();
		}
	}

	private Event getEvent(DataInputStream in) throws IOException
	{
		int len;

		if (in == null)
		{
			Log.e(LOG_TAG, "Requested to receive event, while IPC is down");
			return null;
		}

		try {
			len = in.readInt();
			if (len > evBuffer.length)
			{
				Log.i(LOG_TAG, "Increasing incoming event buffer size to " + len);
				evBuffer = new byte[len];
				evData = new DataInputStream(new ByteArrayInputStream(evBuffer));
			}
			else
				evData.reset();

			in.readFully(evBuffer, 0, len);
			Log.i(LOG_TAG, "Received event from network");
			return new Event(evData);
		}
		catch (EOFException e) {
			Log.w(LOG_TAG, "EOF while receiving an event");
			return null;
		}
	}

	/** Requests to terminate the net client.
	 * @return true if successfully requested, false if client already down
	 */
	public synchronized boolean terminationRequest()
	{
		if (readThread == null || writeThread == null)
			return false;
		
		running = false;
		writeThread.interrupt();
		readThread.interrupt();
		do {
			try {
				if (readThread != null) {
					readThread.join();
					readThread = null;
				}
				if (writeThread != null) {
					writeThread.join();
					writeThread = null;
				}
				break;
			} catch (InterruptedException e) {
				Log.i(LOG_TAG, "Interrupted during wait to finish the network client, retrying...");
			}
		} while (true);
		
		Log.i(LOG_TAG, "Net Client Terminated.");
		return true;
	}

	public boolean sendEvent(Event ev)
	{
		if (!running)
			Log.w(LOG_TAG, "Net Client isn't started, when sending event " + ev.getName());
		return eventQueue.offer(ev);
	}

	/** Abstract method that will be called on receiving a new event */
	abstract protected void receiveEvent(Event ev);
}
