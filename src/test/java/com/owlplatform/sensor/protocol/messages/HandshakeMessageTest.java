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

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

public class HandshakeMessageTest {

	private static final String DEFAULT_HANDSHAKE_STRING = "Sensor Handshake: 21, GRAIL sensor protocol, 0x00, 0x00";

	private HandshakeMessage defaultMessage;

	@Before
	public void createMessage() {
		this.defaultMessage = HandshakeMessage.getDefaultMessage();
		;
	}

	@Test
	public void testDefaultMessage() {
		Assert.assertEquals(HandshakeMessage.PROTOCOL_STRING,
				this.defaultMessage.getProtocolString());
		Assert.assertEquals(HandshakeMessage.PROTOCOL_STRING_LENGTH,
				this.defaultMessage.getStringLength());
		Assert.assertEquals(HandshakeMessage.PROTOCOL_VERSION,
				this.defaultMessage.getVersionNumber());
		Assert.assertEquals(HandshakeMessage.PROTOCOL_RESERVED_BITS,
				this.defaultMessage.getReservedBits());
	}

	@Test
	public void testToString() {
		Assert.assertEquals(DEFAULT_HANDSHAKE_STRING,
				this.defaultMessage.toString());
	}

	@Test
	public void testEqualsObject() {
		Assert.assertFalse(this.defaultMessage.equals(DEFAULT_HANDSHAKE_STRING));
		Object another = HandshakeMessage.getDefaultMessage();
		Assert.assertTrue(this.defaultMessage.equals(another));
	}

	@Test
	public void testEqualsHandshakeMessage() {
		HandshakeMessage another = HandshakeMessage.getDefaultMessage();
		Assert.assertTrue(this.defaultMessage.equals(another));
		Assert.assertTrue(another.equals(this.defaultMessage));

		// Protocol String length
		another.setStringLength(2);
		Assert.assertFalse(this.defaultMessage.equals(another));
		another.setStringLength(HandshakeMessage.PROTOCOL_STRING_LENGTH);
		Assert.assertTrue(another.equals(this.defaultMessage));

		// Protocol String
		another.setProtocolString("1234");
		Assert.assertFalse(this.defaultMessage.equals(another));

		another.setProtocolString(HandshakeMessage.PROTOCOL_STRING);
		Assert.assertTrue(another.equals(this.defaultMessage));

		// Reserved bits
		another.setReservedBits((byte) 1);
		Assert.assertFalse(this.defaultMessage.equals(another));
		another.setReservedBits(HandshakeMessage.PROTOCOL_RESERVED_BITS);
		Assert.assertTrue(another.equals(this.defaultMessage));

		// Version number
		another.setVersionNumber((byte) 1);
		Assert.assertFalse(this.defaultMessage.equals(another));
		another.setVersionNumber(HandshakeMessage.PROTOCOL_VERSION);
		Assert.assertTrue(another.equals(this.defaultMessage));
	}

}
