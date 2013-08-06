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
import org.apache.mina.filter.executor.ExecutorFilter;
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
 */
public class SensorAggregatorInterface {

  /**
   * A wrapper to take hide the IOAdapter events from classes using the
   * SensorAggregatorInterface.
   * 
   * @author Robert Moore
   */
  private static final class AdapterHandler implements SensorIoAdapter {

    /**
     * The actual object that will respond to events.
     */
    private final SensorAggregatorInterface parent;

    /**
     * Creates a new AdapterHandler with the specified
     * {@code SensorAggregatorInterface} to actually handle events.
     * 
     * @param parent
     *          the real event handler
     */
    public AdapterHandler(SensorAggregatorInterface parent) {
      if (parent == null) {
        throw new IllegalArgumentException(
            "SensorAggregatorInterface cannot be null");
      }
      this.parent = parent;
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
      this.parent.exceptionCaught(session, cause);
    }

    @Override
    public void sensorConnected(IoSession session) {
      this.parent.sensorConnected(session);

    }

    @Override
    public void sensorDisconnected(IoSession session) {
      this.parent.sensorDisconnected(session);

    }

    @Override
    public void handshakeMessageReceived(IoSession session,
        HandshakeMessage handshakeMessage) {
      this.parent.handshakeMessageReceived(session, handshakeMessage);
    }

    @Override
    public void handshakeMessageSent(IoSession session,
        HandshakeMessage handshakeMessage) {
      this.parent.handshakeMessageSent(session, handshakeMessage);
    }

    @Override
    public void sensorSampleReceived(IoSession session,
        SampleMessage sampleMessage) {
      this.parent.sensorSampleReceived(session, sampleMessage);
    }

    @Override
    public void sensorSampleSent(IoSession session, SampleMessage sampleMessage) {
      this.parent.sensorSampleSent(session, sampleMessage);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus idleStatus) {
      this.parent.sessionIdle(session, idleStatus);
    }

  }

  /**
   * Logging facility for this class.
   */
  private static final Logger log = LoggerFactory
      .getLogger(SensorAggregatorInterface.class);

  /**
   * The object that will pass events to this SensorAggregatorInterface. Used to
   * hide interface methods.
   */
  private final AdapterHandler handler = new AdapterHandler(this);

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
  protected boolean canSendSamples = false;

  /**
   * How long to wait between connection attempts to the aggregator, in
   * milliseconds.
   */
  protected long connectionRetryDelay = 10000;

  /**
   * Whether or not to try and stay connected to the aggregator.
   */
  protected boolean stayConnected = false;

  /**
   * Whether or not to disconnect from the aggregator if an exception is thrown.
   */
  protected boolean disconnectOnException = true;

  /**
   * The hostname or IP address of the aggregator.
   */
  private String host;

  /**
   * The port number the aggregator is listening on for sensors.
   */
  private int port = 7007;

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
  private SensorIoHandler ioHandler = new SensorIoHandler(this.handler);

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

  /**
   * A MINA filter containing a thread pool for executing message events
   * separate from the IO thread.
   */
  private ExecutorFilter executors;

  /**
   * How long to wait for a connection to the aggregator to be established.
   */
  protected long connectionTimeout = 1000l;

  /**
   * Configures the IO connector to establish a connection to the aggregator.
   * 
   * @return {@code true} if the connector was created successfully, else
   *         {@code false}.
   */
  protected boolean setConnector() {
    if (this.host == null) {
      log.error("No host value set, cannot set up socket connector.");
      return false;
    }
    if (this.port < 0 || this.port > 65535) {
      log.error("Port value is invalid {}.", Integer.valueOf(this.port));
      return false;
    }

    // Disconnect if already connected
    if (this.connector != null) {
      boolean tmp = this.stayConnected;
      this.stayConnected = false;
      this._disconnect();
      this.stayConnected = tmp;
    }

    this.executors = new ExecutorFilter(1);

    this.connector = new NioSocketConnector();
    this.connector.getSessionConfig().setTcpNoDelay(true);
    if (!this.connector.getFilterChain().contains(
        AggregatorSensorProtocolCodecFactory.CODEC_NAME)) {
      this.connector.getFilterChain().addLast(
          AggregatorSensorProtocolCodecFactory.CODEC_NAME,
          new ProtocolCodecFilter(new AggregatorSensorProtocolCodecFactory(
              false)));
    }
    this.connector.getFilterChain().addLast("ExecutorPool", this.executors);

    this.connector.setHandler(this.ioHandler);
    log.debug("Connector set up successfully.");
    return true;
  }

  /**
   * Initiates a connection to the Aggregator (if it is not yet connected). If
   * {@code #stayConnected} is {@code true}, then this method will NOT return
   * until a connection is established or the timeout is exceeded. A timeout of
   * 0 means the configured timeout value will be used.
   * 
   * @param maxWait
   *          how long to wait (in milliseconds) for the connection to
   * @return {@code true} if the connection is established within the timeout
   *         period, else {@code false}.
   */
  public boolean connect(long maxWait) {
    long timeout = maxWait;
    if (timeout <= 0) {
      timeout = this.connectionTimeout;
    }
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
    long waitTime = timeout;
    do {
      long startAttempt = System.currentTimeMillis();
      this.connector.setConnectTimeoutMillis(waitTime - 5);
      if (this._connect(waitTime)) {
        log.debug("Connection succeeded!");
        return true;
      }

      if (this.stayConnected) {
        long retryDelay = this.connectionRetryDelay;
        if (timeout < this.connectionRetryDelay * 2) {
          retryDelay = timeout / 2;
          if (retryDelay < 500) {
            retryDelay = 500;
          }
        }
        try {
          log.warn(String.format(
              "Connection to %s:%d failed, waiting %dms before retrying.",
              this.host, Integer.valueOf(this.port), Long.valueOf(retryDelay)));
          Thread.sleep(retryDelay);
        } catch (InterruptedException ie) {
          // Ignored
        }
        waitTime = waitTime - (System.currentTimeMillis() - startAttempt);
      }
    } while (this.stayConnected && waitTime > 0);

    this._disconnect();
    this.finishConnection();

    return false;

  }

  /**
   * Initiates a connection to the Aggregator (if it is not yet connected). If
   * {@code #stayConnected} is {@code true}, then this method will NOT return
   * until a connection is established. If callers wish to remain connected to
   * the aggregator, it is best to call {@code #setStayConnected(true)} only
   * after calling this method.
   * This method has been replaced by {@link #connect(long)}, and is equivalent
   * to calling {@code #connect(0)}.
   * 
   * @return {@code true} if the connection is established, else {@code false}.
   */
  @Deprecated
  public boolean doConnectionSetup() {
    return this.connect(0);
  }

  /**
   * Shuts down this connection to the aggregator. This method has been replaced
   * by {@link #disconnect()}.
   */
  @Deprecated
  public void doConnectionTearDown() {
    this.disconnect();
  }

  /**
   * Disconnects from the aggregator if already connected.
   */
  public void disconnect() {
    // Make sure we don't automatically reconnect
    this.stayConnected = false;
    this._disconnect();
  }

  /**
   * Establishes a connection to the remote aggregator specified by
   * {@code #host} and {@code #port}, waiting for the {@code timeout} period
   * before giving-up, or an infinite amount of time if {@code timeout} is &le;
   * 0.
   * 
   * @param timeout
   *          the timeout period in milliseconds to wait for the connection
   * @return {@code true} if the connection is established successfully, else
   *         {@code false}.
   */
  protected boolean _connect(long timeout) {
    ConnectFuture connFuture = this.connector.connect(new InetSocketAddress(
        this.host, this.port));
    if (timeout > 0) {
      if (!connFuture.awaitUninterruptibly(timeout)) {
        return false;
      }
    } else {
      connFuture.awaitUninterruptibly();

    }
    if (!connFuture.isConnected()) {
      return false;
    }

    try {
      log.info("Connecting to {}:{}.", this.host, Integer.valueOf(this.port));
      this.session = connFuture.getSession();
    } catch (RuntimeIoException ioe) {
      log.error(String.format("Could not create session to aggregator %s:%d.",
          this.host, Integer.valueOf(this.port)), ioe);
      return false;
    }
    return true;
  }

  /**
   * Disconnects from the aggregator, destroying any sessions and executor
   * filters that are already created. Resets the connection state.
   */
  protected void _disconnect() {
    IoSession currentSession = this.session;
    if (currentSession != null) {
      if (!currentSession.isClosing()) {

        log.info("Closing connection to aggregator at {}.",
            currentSession.getRemoteAddress());
        currentSession.close(true);

      }
      this.session = null;
      this.sentHandshake = null;
      this.receivedHandshake = null;
      this.canSendSamples = false;
      for (ConnectionListener listener : this.connectionListeners) {
        listener.connectionInterrupted(this);
      }
    }
  }

  /**
   * Registers the listener to receive connection-related events. No effect if
   * the listener is already registered.
   * 
   * @param listener
   *          the listener to add.
   */
  public void addConnectionListener(ConnectionListener listener) {
    if (!this.connectionListeners.contains(listener)) {

      this.connectionListeners.add(listener);
    }
  }

  /**
   * Unregisters the listener from receiving connection-related events. No
   * effect if the listener is not registered.
   * 
   * @param listener
   *          the listener to remove.
   */
  public void removeConnectionListener(ConnectionListener listener) {
    if (listener == null) {
      this.connectionListeners.remove(listener);
    }
  }

  /**
   * Verifies the received and notifies listeners if the connection is ready to
   * send samples. If the handshake was invalid, disconnects from the
   * aggregator.
   * 
   * @param session
   *          the session that received the message
   * @param handshakeMessage
   *          the message that was received.
   */
  protected void handshakeMessageReceived(IoSession session,
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
      this._disconnect();
    }

  }

  /**
   * After the handshake is sent, checks to see if a handshake was received. If
   * it was, and it was valid, notifies any listeners that the aggregator is
   * ready to receive samples.
   * 
   * @param session
   *          the session on which the handshake was sent.
   * @param handshakeMessage
   *          the handshake message that was sent.
   */
  protected void handshakeMessageSent(IoSession session,
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
      this._disconnect();
    }
  }

  /**
   * Validates the sent and received handshake messages. If both are present
   * (sent/received, respectively) and valid, then {@code Boolean.TRUE} is
   * returned. If {@code null} is returned, then at least one handshake (sent or
   * received) is missing and the caller may wish to check again in the future.
   * 
   * @return {@code Boolean.TRUE} if the handshakes are exchanged and valid,
   *         {@code Boolean.FALSE} if the handshakes are exchanged and one or
   *         both are invalid, or {@code null} if one or more handshakes is
   *         missing.
   */
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
      this._disconnect();
      this.stayConnected = prevValue;
      return Boolean.FALSE;
    }
    return Boolean.TRUE;

  }

  /**
   * This is an error in the protocol, since the aggregator should not send
   * samples to a sensor.
   * 
   * @param session
   *          the session that received the message.
   * @param sampleMessage
   *          the sample message received.
   */
  protected void sensorSampleReceived(IoSession session,
      SampleMessage sampleMessage) {
    log.error(
        "Protocol error: Received sample message from the aggregator:\n{}",
        sampleMessage);
    this._disconnect();

  }

  /**
   * Called when the session establishes a connection to the aggregator. Writes
   * a handshake to the aggregator.
   * 
   * @param session
   *          the session that connected.
   */
  protected void sensorConnected(IoSession session) {
    if (this.session == null) {
      this.session = session;
    }

    log.info("Connected to {}.", session.getRemoteAddress());

    for (ConnectionListener listener : this.connectionListeners) {
      listener.connectionEstablished(this);
    }

    log.debug("Attempting to write handshake.");
    this.session.write(HandshakeMessage.getDefaultMessage());
  }

  /**
   * Called when the session ends. If {@code #stayConnected} is {@code true},
   * attempts to reconnect to the aggregator.
   * 
   * @param session
   */
  protected void sensorDisconnected(IoSession session) {
    this._disconnect();
    while (this.stayConnected) {
      log.info("Reconnecting to aggregator at {}:{}", this.host,
          Integer.valueOf(this.port));

      try {
        Thread.sleep(this.connectionRetryDelay);
      } catch (InterruptedException ie) {
        // Ignored
      }

      if (this.connect(this.connectionTimeout)) {
        return;
      }

    }

    this.finishConnection();

  }

  /**
   * Cleans-up any session-related constructs when this interface will no longer
   * attempt connections to the aggregator. Notifies listener that the
   * connection has ended permanently.
   */
  protected void finishConnection() {
    this.connector.dispose();
    this.connector = null;
    for (ConnectionListener listener : this.connectionListeners) {
      listener.connectionEnded(this);
    }
    if (this.executors != null) {
      this.executors.destroy();
    }
  }

  /**
   * No action taken, since there is not a keep-alive in the protocol.
   * 
   * @param session
   *          the session that is idle.
   * @param idleStatus
   *          whether the sender, receiver, or both are idle.
   */
  protected void sessionIdle(IoSession session, IdleStatus idleStatus) {
    // Nothing to do
  }

  /**
   * Returns the wait interval (in milliseconds) between attempts to connect to
   * the aggregator.
   * 
   * @return the connection retry interval in milliseconds.
   */
  public long getConnectionRetryDelay() {
    return this.connectionRetryDelay;
  }

  /**
   * Sets the aggregator connection retry interval in milliseconds.
   * 
   * @param connectionRetryDelay
   *          the new connection retry interval value.
   */
  public void setConnectionRetryDelay(long connectionRetryDelay) {
    this.connectionRetryDelay = connectionRetryDelay;
  }

  /**
   * Indicates whether this {@code SensorAggregatorInterface} will automatically
   * reconnect when the connection is lost. This value is {@code false} by
   * default.
   * 
   * @return {@code true} if an automatic reconnect will be attempted, else
   *         {@code false}.
   */
  public boolean isStayConnected() {
    return this.stayConnected;
  }

  /**
   * Sets whether or not this {@code SensorAggregatorInterface} will
   * automatically reconnect when the connection is lost.
   * 
   * @param stayConnected
   *          the new value.
   */
  public void setStayConnected(boolean stayConnected) {
    this.stayConnected = stayConnected;
  }

  /**
   * Indicates whether this {@code SensorAggregatorInterface} should disconnect
   * when an exception is caught. The default value is true.
   * 
   * @return {@code true} if exceptions will cause a disconnect event.
   */
  public boolean isDisconnectOnException() {
    return this.disconnectOnException;
  }

  /**
   * Sets whether this {@code SensorAggregatorInterface} should disconnect when
   * exceptions are encountered.
   * 
   * @param disconnectOnException
   *          the new value.
   */
  public void setDisconnectOnException(boolean disconnectOnException) {
    this.disconnectOnException = disconnectOnException;
  }

  /**
   * Returns the configured hostname for the aggregator.
   * 
   * @return the configured hostname for the aggregator, or {@code null} if none
   *         is set.
   */
  public String getHost() {
    return this.host;
  }

  /**
   * Sets the hostname/IP address for the aggregator. Changes to this value only
   * take effect on the next attempt to connect.
   * 
   * @param host
   *          the new hostname/IP address for the aggregator.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Returns the configured port number for the aggregator.
   * 
   * @return the configured port number for the aggregator, or -1 if it has not
   *         been set.
   */
  public int getPort() {
    return this.port;
  }

  /**
   * Sets the port number for the aggregator. Changes to this value will not
   * take effect until the next connection attempt.
   * 
   * @param port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Simply logs that a sample was sent.
   * 
   * @param session
   *          the session on which the message was sent.
   * @param sampleMessage
   *          the sample that was sent.
   */
  protected void sensorSampleSent(IoSession session, SampleMessage sampleMessage) {
    log.debug("Sent {}", sampleMessage);

  }

  /**
   * Sends a sample to the aggregator.
   * 
   * @param sampleMessage
   *          the sample to send
   * @return {@code true} if the sample was written, else {@code false}.
   */
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

  /**
   * Responds to exceptions and errors on the aggregator session. If
   * {@code #disconnectOnException} is {@code true}, then disconnects from the
   * aggregator. An OutOfMemory error will force a JVM exit.
   * 
   * @param session
   *          the session that threw the exception or error.
   * @param cause
   *          the Throwable that was thrown.
   */
  protected void exceptionCaught(IoSession session, Throwable cause) {
    log.error("Exception while communicating with " + this + ".", cause);
    // Nothing to do if we have no memory
    if (cause instanceof OutOfMemoryError) {
      System.exit(1);
    }
    if (this.disconnectOnException) {
      this._disconnect();
    }
  }

  /**
   * Returns {@code true} if the connection is ready to send sample messages.
   * Classes that wish to be notified asynchronously when samples can be sent,
   * should register as a {@code ConnectionListener}.
   * 
   * @return {@code true} if samples can be sent, else {@code false}.
   */
  public boolean isCanSendSamples() {
    if (!this.canSendSamples || this.session == null) {
      return false;
    }

    return this.session.getScheduledWriteMessages() < this.maxOutstandingSamples;
  }

  /**
   * The maximum number of sample messages that are allowed to be buffered
   * before causing a send failure.
   * 
   * @return the current maximum number of bufferable sample messages.
   */
  public int getMaxOutstandingSamples() {
    return this.maxOutstandingSamples;
  }

  /**
   * Sets the maximum number of sample messages allowed to be buffered.
   * 
   * @param maxOutstandingSamples
   *          the new maximum value.
   */
  public void setMaxOutstandingSamples(int maxOutstandingSamples) {
    this.maxOutstandingSamples = maxOutstandingSamples;
  }

  @Override
  public String toString() {
    return "Sensor-Aggregator Interface @ " + this.host + ":" + this.port;
  }

}
