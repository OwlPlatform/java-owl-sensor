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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for SensorAggregatorInterface.
 * @author Robert Moore
 *
 */
public class SensorAggregatorInterfaceTest {

  /**
   * Connection retry delay value.
   */
  private static final long CONN_RETRY_DELAY = 5000l;
  
  /**
   * Connection timeout value.
   */
  private static final long CONN_TIMEOUT_DELAY = 7879l;
  
  /**
   * Maximum number of outstanding samples allowed.
   */
  private static final int MAX_SAMPLES = 500;
  
  /**
   * Test port for sensor connections.
   */
  private static final int CONN_PORT = 18007;
  
  /**
   * Localhost for sensor connections.
   */
  private static final String CONN_HOST = "localhost";
  
  /**
   * Interface to use while testing.
   */
  private SensorAggregatorInterface testInterface;
  
  
  
  
  /**
   * Initializes the aggregator interface.
   */
  @Before
  public void createInterface(){
    this.testInterface = new SensorAggregatorInterface();
  }
  
  /**
   * Tests the setters/getters of {@code SensorAggregatorInterface}.
   */
  @Test
  public void testBasicSettings(){
    this.testInterface.setConnectionRetryDelay(CONN_RETRY_DELAY);
    Assert.assertEquals(CONN_RETRY_DELAY, this.testInterface.getConnectionRetryDelay());
    
    this.testInterface.setMaxOutstandingSamples(MAX_SAMPLES);
    Assert.assertEquals(MAX_SAMPLES, this.testInterface.getMaxOutstandingSamples());
    
    this.testInterface.setPort(CONN_PORT);
    Assert.assertEquals(CONN_PORT,this.testInterface.getPort());
    
    this.testInterface.setHost(CONN_HOST);
    Assert.assertEquals(CONN_HOST, this.testInterface.getHost());
    
    this.testInterface.setDisconnectOnException(true);
    this.testInterface.setStayConnected(false);
    
    Assert.assertFalse(this.testInterface.connect(CONN_TIMEOUT_DELAY));
    
    
  }
  

}
