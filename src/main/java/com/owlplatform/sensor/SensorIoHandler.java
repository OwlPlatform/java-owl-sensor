/*
 * Owl Platform Sensor-Aggregator Library for Java
 * Copyright (C) 2012 Robert Moore and the Owl Platform
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.owlplatform.sensor;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

public class SensorIoHandler implements IoHandler {

	private static final Logger log = LoggerFactory
			.getLogger(SensorIoHandler.class);

	protected SensorIoAdapter sensorIoAdapter;

	public SensorIoHandler(SensorIoAdapter sensorIoAdapter) {
		this.sensorIoAdapter = sensorIoAdapter;
	}

	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		log.error("Unhandled exception caught in session {}: {}", session,
				cause);

	}

	public void messageReceived(IoSession session, Object message)
			throws Exception {
		log.debug("{} <-- {}", session, message);
		if (this.sensorIoAdapter == null) {
			log.warn(
					"No SensorIoAdapter defined. Ignoring message from {}: {}",
					session, message);
			return;
		}

		if (message instanceof SampleMessage) {
			if (this.sensorIoAdapter != null) {
				this.sensorIoAdapter.sensorSampleReceived(session,
						(SampleMessage) message);
			}
		} else if (message instanceof HandshakeMessage) {
			log.debug("Received handshake message from {}: {}", session,
					message);
			if (this.sensorIoAdapter != null) {
				this.sensorIoAdapter.handshakeMessageReceived(session,
						(HandshakeMessage) message);
			}

		} else {
			log.warn("Unhandled message type for session {}: {}", session,
					message);
		}

	}

	public void messageSent(IoSession session, Object message) throws Exception {

		log.debug("{} --> {}", message, session);
		if (this.sensorIoAdapter == null) {
			log.warn("No SensorIoAdapter defined. Ignoring message to {}: {}",
					session, message);
			return;
		}

		if (message instanceof HandshakeMessage) {
			log.debug("Handshake message sent to {}: {}", session, message);
			if (this.sensorIoAdapter != null) {
				this.sensorIoAdapter.handshakeMessageSent(session,
						(HandshakeMessage) message);
			}
		} else if (message instanceof SampleMessage) {
			if (this.sensorIoAdapter != null) {
				this.sensorIoAdapter.sensorSampleSent(session,
						(SampleMessage) message);
			}
		} else {
			log.warn("Unhandled message type sent to {}: {}", session, message);
		}

	}

	public void sessionClosed(IoSession session) throws Exception {
		log.debug("Session closed for sensor {}.", session);
		if (this.sensorIoAdapter != null) {
			this.sensorIoAdapter.sensorDisconnected(session);
		}
	}

	public void sessionCreated(IoSession session) throws Exception {
		// Handle sessionOpened events instead
	}

	public void sessionIdle(IoSession session, IdleStatus status)
			throws Exception {
		log.debug("Sensor session for {} is idle: {}", session, status);
		if (this.sensorIoAdapter != null) {
			this.sensorIoAdapter.sessionIdle(session, status);
		}

	}

	public void sessionOpened(IoSession session) throws Exception {
		log.debug("Session opened for sensor {}.", session);
		if (this.sensorIoAdapter != null) {
			this.sensorIoAdapter.sensorConnected(session);
		}
	}

}
