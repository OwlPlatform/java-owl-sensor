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


import com.owlplatform.common.SampleMessage;
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;


/**
 * Defines an IO adapter to be used by classes that respond to Sensor-Aggregator message events.
 * @author Robert Moore
 *
 */
public interface SensorIoAdapter
{
	/**
	 * Called when an exception is thrown by the underlying IO.
	 * @param session
	 * @param cause
	 */
	public void exceptionCaught(IoSession session, Throwable cause);
	
    /**
     * Called when a sensor connects to the aggregator
     * @param session
     */
	public void sensorConnected(IoSession session);
    
	/**
	 * Called when a sensor disconnects from the aggregator.
	 * @param session
	 */
    public void sensorDisconnected(IoSession session);
    
    /**
     * Called when a sensor sends a Handshake message to the aggregator.
     * @param session
     * @param handshakeMessage
     */
    public void handshakeMessageReceived(IoSession session, HandshakeMessage handshakeMessage);
    
    /**
     * Called after a Handshake message is sent to a sensor.
     * @param session
     * @param handshakeMessage
     */
    public void handshakeMessageSent(IoSession session, HandshakeMessage handshakeMessage);
    
    /**
     * Called after a sample message is received.
     * @param session
     * @param sampleMessage
     */
    public void sensorSampleReceived(IoSession session, SampleMessage sampleMessage);
    
    /**
     * Called after a sample message is sent.
     * @param session
     * @param sampleMessage
     */
    public void sensorSampleSent(IoSession session, SampleMessage sampleMessage);
    
    /**
     * Called when a sensor session becomes idle.
     * @see IoHandler#sessionIdle(IoSession, IdleStatus)
     * @param session
     * @param idleStatus
     */
    public void sessionIdle(IoSession session, IdleStatus idleStatus);
}
