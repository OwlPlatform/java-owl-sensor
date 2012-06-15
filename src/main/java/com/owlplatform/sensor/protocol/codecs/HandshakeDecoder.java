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

import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

/**
 * Decodes a Sensor-Aggregator handshake message.
 * 
 * @author Robert Moore
 * @see HandshakeMessage
 */
public class HandshakeDecoder implements MessageDecoder {

  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory
      .getLogger(HandshakeDecoder.class);

  /**
   * Connection state key to track if the handshake was decoded on this session
   * yet.
   */
  public static final String CONN_STATE_KEY = HandshakeDecoder.class.getName()
      + ".STATE";

  /**
   * Tracks whether a handshake message was received or not.
   * 
   * @author Robert Moore
   * 
   */
  static final class HubConnectionState {
    /**
     * Flag to indicate that a handshake was decoded.
     */
    boolean handshakeReceived = false;
  }

  /**
   * Charset for encoding the protocol name.
   */
  private static final Charset charsetASCII = Charset.forName("US-ASCII");

  @Override
  public MessageDecoderResult decodable(IoSession arg0, IoBuffer arg1) {
    HubConnectionState connState = (HubConnectionState) arg0
        .getAttribute(CONN_STATE_KEY);

    if (connState == null) {
      log.debug("Creating new handshake connection state for {}.", arg0);
      connState = new HubConnectionState();
      connState.handshakeReceived = false;
      arg0.setAttribute(CONN_STATE_KEY, connState);
    }

    if (connState.handshakeReceived) {
      return MessageDecoderResult.NOT_OK;
    }

    if (!arg1.prefixedDataAvailable(4, HandshakeMessage.PROTOCOL_STRING_LENGTH)) {
      log.debug("Not yet decodable with only {} bytes.",
          Integer.valueOf(arg1.remaining()));
      return MessageDecoderResult.NEED_DATA;
    }

    // TODO: Need better logic to determine decodability
    return MessageDecoderResult.OK;
  }

  @Override
  public MessageDecoderResult decode(IoSession arg0, IoBuffer arg1,
      ProtocolDecoderOutput arg2) throws Exception {
    HubConnectionState connState = (HubConnectionState) arg0
        .getAttribute(CONN_STATE_KEY);
    if (connState == null) {
      log.debug("Creating new handshake connection state for {}.", arg0);
      connState = new HubConnectionState();
      connState.handshakeReceived = false;
      arg0.setAttribute(CONN_STATE_KEY, connState);
    }

    // If handshake already received, skip this decoder
    if (connState.handshakeReceived) {
      log.warn("Handshake already received.");
      return MessageDecoderResult.NOT_OK;
    }

    if (arg1.prefixedDataAvailable(4, HandshakeMessage.PROTOCOL_STRING_LENGTH)) {
      HandshakeMessage message = new HandshakeMessage();
      message.setStringLength(arg1.getInt());
      if (message.getStringLength() != HandshakeMessage.PROTOCOL_STRING_LENGTH) {
        throw new RuntimeException(String.format(
            "Handshake protocol string length is incorrect: %d",
            Integer.valueOf(message.getStringLength())));
      }

      message
          .setProtocolString(String.valueOf(arg1.getString(
              message.getStringLength(),
              HandshakeDecoder.charsetASCII.newDecoder())));
      message.setVersionNumber(arg1.get());
      message.setReservedBits(arg1.get());

      arg2.write(message);
      log.debug("Decoded {}.", message);
      connState.handshakeReceived = true;
      return MessageDecoderResult.OK;
    }
    // Entire message is not yet available
    log.warn("Insufficient buffer size: {}.", Integer.valueOf(arg1.remaining()));
    return MessageDecoderResult.NEED_DATA;
  }

  @Override
  public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
      throws Exception {
    // Nothing to do
  }

}
