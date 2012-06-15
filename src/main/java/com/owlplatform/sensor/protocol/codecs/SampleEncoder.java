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

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

import com.owlplatform.common.SampleMessage;

/**
 * Encodes a Sensor-Aggregator sample message
 * 
 * @author Robert Moore
 * 
 */
public class SampleEncoder implements MessageEncoder<SampleMessage> {

  @Override
  public void encode(IoSession session, SampleMessage message,
      ProtocolEncoderOutput out) throws Exception {
    if (message.getLengthPrefixSensor() < 0) {
      throw new IOException("Message length is negative.");
    }

    IoBuffer buffer = IoBuffer.allocate(message.getLengthPrefixSensor() + 4);

    buffer.putInt(message.getLengthPrefixSensor());
    buffer.put(message.getPhysicalLayer());
    buffer.put(message.getDeviceId());
    buffer.put(message.getReceiverId());
    buffer.putLong(message.getReceiverTimeStamp());
    buffer.putFloat(message.getRssi());
    if (message.getSensedData() != null) {
      buffer.put(message.getSensedData());
    }

    buffer.flip();

    out.write(buffer);
    out.flush();
    buffer.free();
  }

}
