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

import org.apache.mina.filter.codec.demux.DemuxingProtocolCodecFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

/**
 * Creates a new protocol codec factory for the Sensor-Aggregator protocol.
 * 
 * @author Robert Moore
 * 
 */
public class AggregatorSensorProtocolCodecFactory extends
    DemuxingProtocolCodecFactory {

  /**
   * Identifier for the protocol codec chain.
   */
  public static final String CODEC_NAME = "Owl Platform Sensor-Aggregator codec";

  /**
   * Factory to generate a Sensor-Aggregator protocol code.
   * 
   * @param server
   *          {@code true} if the codec should be for the aggregator, or
   *          {@code false} for sensors.
   */
  public AggregatorSensorProtocolCodecFactory(boolean server) {
    super();
    if (server) {
      super.addMessageEncoder(HandshakeMessage.class, HandshakeEncoder.class);

      super.addMessageDecoder(SampleDecoder.class);
      super.addMessageDecoder(HandshakeDecoder.class);
    } else {
      super.addMessageEncoder(HandshakeMessage.class, HandshakeEncoder.class);
      super.addMessageDecoder(HandshakeDecoder.class);
      super.addMessageEncoder(SampleMessage.class, SampleEncoder.class);
    }
  }

}
