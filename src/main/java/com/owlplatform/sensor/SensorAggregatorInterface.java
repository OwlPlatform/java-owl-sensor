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

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.sensor.listeners.ConnectionListener;
import com.owlplatform.sensor.protocol.codecs.AggregatorSensorProtocolCodecFactory;
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

/**
 * A convenience class for interfacing with aggregators as a sensor/hub. Since
 * most sensors are implemented in C/C++, this class is primarily used by
 * Aggregators to forward sensor data to another aggregator.
 * 
 * @author Robert Moore
 * 
 */
public class SensorAggregatorInterface implements SensorIoAdapter {
  /**
   * Logging facility for this class.
   */
  private static final Logger log = LoggerFactory
      .getLogger(SensorAggregatorInterface.class);

  /**
   * The handshake sent to the aggregator.
   */
  private HandshakeMessage sentHandshake;

  /**
   * The handshake received from the aggregator.
   */
  private HandshakeMessage receivedHandshake;

  /**
   * Indicates whether handshakes have been exchanged/checked and the aggregator
   * can receive sample messages.
   */
  private boolean canSendSamples = false;

  /**
   * How long to wait when connecting and disconnecting from the aggregator, in
   * milliseconds.
   */
  private long connectionTimeout = 10000;

  /**
   * How long to wait between connection attempts to the aggregator, in
   * milliseconds.
   */
  private long connectionRetryDelay = 10000;

  /**
   * Whether or not to try and stay connected to the aggregator.
   */
  private boolean stayConnected = false;

  /**
   * Whether or not to disconnect from the aggregator if an exception is thrown.
   */
  private boolean disconnectOnException = true;

  /**
   * The hostname or IP address of the aggregator.
   */
  private String host;

  /**
   * The port number the aggregator is listening on for sensors.
   */
  private int port;

  /**
   * The session of the connected aggregator, or {@code null} if no connection
   * is established.
   */
  private IoSession session;

  /**
   * SocketConnector used to connect to the aggregator.
   */
  private SocketConnector connector;

  /**
   * IoHandler used by this aggregator interface.
   */
  private SensorIoHandler ioHandler = new SensorIoHandler(this);

  /**
   * List (queue) of listeners for connection events for this aggregator
   * interface.
   */
  private ConcurrentLinkedQueue<ConnectionListener> connectionListeners = new ConcurrentLinkedQueue<ConnectionListener>();

  /**
   * Number of outstanding (buffered) sample messages to be sent on the
   * Aggregator's session.
   */
  private int maxOutstandingSamples = Integer.MAX_VALUE;

  private abstract static class ConnectionProcessor implements Runnable {
    protected SensorAggregatorInterface aggregator;

    public ConnectionProcessor(SensorAggregatorInterface aggregator) {
      this.aggregator = aggregator;
    }
  }

  protected boolean setConnector() {
    if (this.host == null) {
      log.error("No host value set, cannot set up socket connector.");
      return false;
    }
    if (this.port < 0 || this.port > 65535) {
      log.error("Port value is invalid {}.", this.port);
      return false;
    }

    connector = new NioSocketConnector();
    connector.getSessionConfig().setTcpNoDelay(true);
    if (!connector.getFilterChain().contains(
        AggregatorSensorProtocolCodecFactory.CODEC_NAME)) {
      connector.getFilterChain().addLast(
          AggregatorSensorProtocolCodecFactory.CODEC_NAME,
          new ProtocolCodecFilter(new AggregatorSensorProtocolCodecFactory(
              false)));
    }
    connector.setHandler(this.ioHandler);
    log.debug("Connector set up successfully.");
    return true;
  }

  /**
   * Initiates a connection to the Aggregator (if it is not yet connected).
   * 
   * @return true if the connection is established.
   */
  public boolean doConnectionSetup() {
    if (this.connector == null) {
      if (!this.setConnector()) {
        log.error("Unable to set up connection to the aggregator.");
        return false;
      }
    }

    if (this.session != null) {
      log.error("Already connected!");
      return false;
    }

    do {
      if (this.connect()) {
        log.debug("Connection succeeded!");
        return true;
      }

      if (this.stayConnected) {
        try {
          log.warn(String.format(
              "Connection to %s:%d failed, waiting %dms before retrying.",
              this.host, this.port, this.connectionRetryDelay));
          Thread.sleep(this.connectionRetryDelay);
        } catch (InterruptedException ie) {
          // Ignored
        }
      }

    } while (this.stayConnected);

    this.disconnect();
    this.finishConnection();

    return false;
  }

  public void doConnectionTearDown() {
    // Make sure we don't automatically reconnect
    this.stayConnected = false;
    this.disconnect();
  }

  protected boolean connect() {

    ConnectFuture connFuture = this.connector.connect(new InetSocketAddress(
        this.host, this.port));
    if (!connFuture.awaitUninterruptibly(connectionTimeout)) {
      return false;
    }
    if (!connFuture.isConnected()) {
      return false;
    }

    try {
      log.debug("Attempting connection to {}:{}.", this.host, this.port);
      this.session = connFuture.getSession();
    } catch (RuntimeIoException ioe) {
      log.error(String.format("Could not create session to aggregator %s:%d.",
          this.host, this.port), ioe);
      return false;
    }
    return true;
  }

  protected void disconnect() {
    if (this.session != null) {
      log.debug("Closing connection to aggregator at {} (waiting {}ms).",
          this.session.getRemoteAddress(), this.connectionTimeout);
      this.session.close(false);
      this.session = null;
      this.sentHandshake = null;
      this.receivedHandshake = null;
      this.canSendSamples = false;
      for (ConnectionListener listener : this.connectionListeners) {
        listener.connectionInterrupted(this);
      }
    }
  }

  public void addConnectionListener(ConnectionListener listener) {
    this.connectionListeners.add(listener);
  }

  public void removeConnectionListener(ConnectionListener listener) {
    this.connectionListeners.remove(listener);
  }

  public void handshakeMessageReceived(IoSession session,
      HandshakeMessage handshakeMessage) {
    log.debug("Received {}", handshakeMessage);
    this.receivedHandshake = handshakeMessage;
    Boolean handshakeCheck = this.checkHandshake();
    if (handshakeCheck == null) {
      return;
    }
    if (Boolean.TRUE.equals(handshakeCheck)) {
      this.canSendSamples = true;
      for (ConnectionListener listener : this.connectionListeners) {
        listener.readyForSamples(this);
      }
    } else if (Boolean.FALSE.equals(handshakeCheck)) {
      log.warn("Handshakes did not match.");
      this.disconnect();
    }

  }

  public void handshakeMessageSent(IoSession session,
      HandshakeMessage handshakeMessage) {
    log.debug("Sent {}", handshakeMessage);
    this.sentHandshake = handshakeMessage;
    Boolean handshakeCheck = this.checkHandshake();
    if (handshakeCheck == null) {
      return;
    }
    if (Boolean.TRUE.equals(handshakeCheck)) {
      this.canSendSamples = true;
      for (ConnectionListener listener : this.connectionListeners) {
        listener.readyForSamples(this);
      }
    } else if (Boolean.FALSE.equals(handshakeCheck)) {
      log.warn("Handshakes did not match.");
      this.disconnect();
    }
  }

  protected Boolean checkHandshake() {
    if (this.sentHandshake == null) {
      log.debug("Sent handshake is null, not checking.");
      return null;
    }
    if (this.receivedHandshake == null) {
      log.debug("Received handshake is null, not checking.");
      return null;
    }

    if (!this.sentHandshake.equals(this.receivedHandshake)) {
      log.error(
          "Handshakes do not match.  Closing connection to distributor at {}.",
          this.session.getRemoteAddress());
      boolean prevValue = this.stayConnected;
      this.stayConnected = false;
      this.disconnect();
      this.stayConnected = prevValue;
      return Boolean.FALSE;
    }
    return Boolean.TRUE;

  }

  public void sensorSampleReceived(IoSession session,
      SampleMessage sampleMessage) {
    log.error(
        "Protocol error: Received sample message from the aggregator:\n{}",
        sampleMessage);
    this.disconnect();

  }

  public void sensorConnected(IoSession session) {
    if (this.session == null) {
      log.warn("Session was not correctly stored during connection set-up.");
      this.session = session;
    }

    log.info("Connected to {}.", session.getRemoteAddress());

    for (ConnectionListener listener : this.connectionListeners) {
      listener.connectionEstablished(this);
    }

    log.debug("Attempting to write handshake.");
    this.session.write(HandshakeMessage.getDefaultMessage());
  }

  public void sensorDisconnected(IoSession session) {
    this.disconnect();
    if (this.stayConnected) {
      log.info("Reconnecting to distributor at {}:{}", this.host, this.port);
      new Thread(new ConnectionProcessor(this) {

        public void run() {
          if (this.aggregator.doConnectionSetup()) {
            return;
          }
          this.aggregator.finishConnection();
        }
      }, "Reconnect Thread").start();
    } else {
      this.finishConnection();
    }

  }

  void finishConnection() {
    this.connector.dispose();
    this.connector = null;
    for (ConnectionListener listener : this.connectionListeners) {
      listener.connectionEnded(this);
    }
  }

  public void sessionIdle(IoSession session, IdleStatus idleStatus) {
    // TODO Auto-generated method stub

  }

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public long getConnectionRetryDelay() {
    return connectionRetryDelay;
  }

  public void setConnectionRetryDelay(long connectionRetryDelay) {
    this.connectionRetryDelay = connectionRetryDelay;
  }

  public boolean isStayConnected() {
    return stayConnected;
  }

  public void setStayConnected(boolean stayConnected) {
    this.stayConnected = stayConnected;
  }

  public boolean isDisconnectOnException() {
    return disconnectOnException;
  }

  public void setDisconnectOnException(boolean disconnectOnException) {
    this.disconnectOnException = disconnectOnException;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void sensorSampleSent(IoSession session, SampleMessage sampleMessage) {
    log.debug("Sent {}", sampleMessage);

  }

  public boolean sendSample(SampleMessage sampleMessage) {
    if (!this.canSendSamples) {
      log.warn("Cannot send samples.");
      return false;
    }

    if (this.session.getScheduledWriteMessages() > this.maxOutstandingSamples) {
      log.warn("Buffer full, cannot send sample.");
      return false;
    }
    this.session.write(sampleMessage);
    return true;
  }

  @Override
  public void exceptionCaught(IoSession session, Throwable cause) {
   log.error("Exception while communicating with " + this + ".", cause);
   this.disconnect();
  }

  public boolean isCanSendSamples() {
    return canSendSamples;
  }

  public int getMaxOutstandingSamples() {
    return maxOutstandingSamples;
  }

  public void setMaxOutstandingSamples(int maxOutstandingSamples) {
    this.maxOutstandingSamples = maxOutstandingSamples;
  }

  @Override
  public String toString() {
    return "Sensor-Aggregator Interface @ " + this.host + ":" + this.port;
  }

}
