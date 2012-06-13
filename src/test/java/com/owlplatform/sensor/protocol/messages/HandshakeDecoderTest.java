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
package com.owlplatform.sensor.protocol.messages;

import static org.junit.Assert.*;

import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

import com.owlplatform.sensor.protocol.codecs.HandshakeDecoder;
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

public class HandshakeDecoderTest {

	//	public static final String PROTOCOL_STRING = "GRAIL sensor protocol";
	private static final byte[] HANDSHAKE_BYTES = new byte[]{0x0, 0x0, 0x0, (byte)HandshakeMessage.PROTOCOL_STRING_LENGTH, 'G','R','A','I','L',' ','s','e','n','s','o','r',' ','p','r','o','t','o','c','o','l',HandshakeMessage.PROTOCOL_VERSION,HandshakeMessage.PROTOCOL_RESERVED_BITS}; 
	
	@Test
	public void test() {
		IoSession session = new DummySession();
		HandshakeDecoder decoder = new HandshakeDecoder();
		
	}

}
