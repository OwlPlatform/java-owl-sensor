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

package com.owlplatform.sensor.protocol.codecs;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

/**
 * Encodes a Sensor-Aggregator handshake message.
 * 
 * @author Robert Moore
 * 
 */
public class HandshakeEncoder implements MessageEncoder<HandshakeMessage> {

  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory
      .getLogger(HandshakeEncoder.class);

  /**
   * State key to track if this session already encoded a handshake message.
   */
  private static final String CONN_STATE_KEY = HandshakeEncoder.class.getName()
      + ".STATE";

  /**
   * State for tracking handshake encoding.
   * 
   * @author Robert Moore
   * 
   */
  private static final class HubConnectionState {
    /**
     * Create a HubConnectionState.
     */
    public HubConnectionState() {
    }

    /**
     * Flag to indicate that a handshake was encoded.
     */
    boolean handshakeSent = false;
  }

  

  @Override
  public void encode(IoSession session, HandshakeMessage message,
      ProtocolEncoderOutput out) throws Exception {
    HubConnectionState connState = (HubConnectionState) session
        .getAttribute(CONN_STATE_KEY);
    if (connState == null) {
      connState = new HubConnectionState();
      connState.handshakeSent = false;
      session.setAttribute(CONN_STATE_KEY, connState);
    }

    log.debug("Sending handshake message.");

    if (connState.handshakeSent) {
      log.warn("Handshake already sent.");
      return;
    }

    IoBuffer buffer = IoBuffer.allocate(HandshakeMessage.MESSAGE_LENGTH);

    buffer.putInt(message.getStringLength());
    buffer.put(message.getProtocolString().getBytes("ASCII"));
    buffer.put(message.getVersionNumber());
    buffer.put(message.getReservedBits());

    buffer.flip();

    out.write(buffer);

    connState.handshakeSent = true;

  }

}
