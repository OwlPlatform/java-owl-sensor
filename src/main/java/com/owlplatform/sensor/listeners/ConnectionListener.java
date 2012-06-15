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

package com.owlplatform.sensor.listeners;

import com.owlplatform.sensor.SensorAggregatorInterface;

/**
 * ConnectionListeners will be notified of connection-related events for a
 * Sensor connecting to an aggregator. These events include connection
 * establishment, end, interruption, and that the Aggregator is ready to receive
 * samples.
 * 
 * @author Robert Moore
 * 
 */
public interface ConnectionListener {
  /**
   * Called when a connection to the aggregator is terminated and will not be
   * reestablished.
   * 
   * @param aggregator
   */
  public void connectionEnded(SensorAggregatorInterface aggregator);

  /**
   * Called when a connection to the aggregator is opened.
   * 
   * @param aggregator
   */
  public void connectionEstablished(SensorAggregatorInterface aggregator);

  /**
   * Called when a connection to the aggregator is terminated, but may be
   * reestablished.
   * 
   * @param aggregator
   */
  public void connectionInterrupted(SensorAggregatorInterface aggregator);

  /**
   * Called after the handshake messages have been exchanged and checked.
   * Samples may be sent to the aggregator.
   * 
   * @param aggregator
   */
  public void readyForSamples(SensorAggregatorInterface aggregator);
}
