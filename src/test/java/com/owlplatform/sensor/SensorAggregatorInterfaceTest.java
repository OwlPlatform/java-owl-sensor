package com.owlplatform.sensor;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.sensor.protocol.codecs.AggregatorSensorProtocolCodecFactory;

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
    
    Assert.assertFalse(this.testInterface.doConnectionSetup());
    
    
  }
  
  @Test
  public void test() {
    
  }

}
