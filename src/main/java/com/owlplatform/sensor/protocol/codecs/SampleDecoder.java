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
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;

public class SampleDecoder implements MessageDecoder {

	private static final Logger log = LoggerFactory
			.getLogger(SampleDecoder.class);

	public MessageDecoderResult decodable(IoSession arg0, IoBuffer arg1) {

		// TODO: Decide on some max limit 64k is IP
		if (arg1.prefixedDataAvailable(4, 65536)) {
			arg1.mark();
			int messageLength = arg1.getInt();
			if (messageLength < 1) {
				arg1.reset();
				return MessageDecoderResult.NOT_OK;
			}

			byte messageType = arg1.get();
			arg1.reset();

			return MessageDecoderResult.OK;

		}
		return MessageDecoderResult.NEED_DATA;
	}

	public MessageDecoderResult decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		if (!in.prefixedDataAvailable(4, 65535)) {
			return MessageDecoderResult.NEED_DATA;
		}

		SampleMessage message = new SampleMessage();
		int remLength = in.getInt();
		log.debug("Message length: {}", remLength);

		message.setPhysicalLayer(in.get());
		--remLength;

		byte[] buff = new byte[SampleMessage.DEVICE_ID_SIZE];
		in.get(buff);
		message.setDeviceId(buff);
		remLength -= SampleMessage.DEVICE_ID_SIZE;

		buff = new byte[SampleMessage.DEVICE_ID_SIZE];
		in.get(buff);
		message.setReceiverId(buff);
		remLength -= SampleMessage.DEVICE_ID_SIZE;

		message.setReceiverTimeStamp(in.getLong());
		remLength -= 8;
		message.setRssi(in.getFloat());
		remLength -= 4;

		// Check to see if there is sensor payload attached
		if (remLength > 0) {
			buff = new byte[remLength];
			in.get(buff, 0, remLength);
			message.setSensedData(buff);
		}

		out.write(message);

		return MessageDecoderResult.OK;
	}

	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
			throws Exception {
		// TODO Auto-generated method stub

	}
}
