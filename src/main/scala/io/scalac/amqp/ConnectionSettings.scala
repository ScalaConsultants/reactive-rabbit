package io.scalac.amqp

import scala.collection.immutable.Seq
import scala.concurrent.duration._


/** Hostname/port pair. */
final case class Address(
  /** The port to use when connecting to the broker. */
  host: String,

  /** The port to use when connecting to the broker. */
  port: Int) {

  require(port > 0, "port <= 0")
}

object ConnectionSettings {
  /** Create settings with some sane default values.
    * Applicable only for connecting to RabbitMQ broker running on localhost. */
  def apply(): ConnectionSettings = apply(
    addresses = Seq(Address(host = "localhost", port = 5672)),
    virtualHost = "/",
    username = "guest",
    password = "guest",
    requestedHeartbeat = None,
    connectionTimeout = Duration.Inf,
    networkRecoveryInterval = 5.seconds
  )
}

/** List of settings required to establish connection to the broker. */
final case class ConnectionSettings(

  /** An immutable sequence of known broker addresses (hostname/port pairs)
    * to try in order. A random one will be picked during recovery. */
  addresses: Seq[Address],

  /** Virtual host to use when connecting to the broker. */
  virtualHost: String,

  /** User name to use when connecting to the broker. */
  username: String,

  /** Password to use when connecting to the broker. */
  password: String,

  /** Requested heartbeat interval, at least 1 second.
    * [[None]] to disable heartbeat. */
  requestedHeartbeat: Option[FiniteDuration],

  /** The default connection timeout, at least 1 millisecond. */
  connectionTimeout: Duration,

  /** How long will automatic recovery wait before attempting to reconnect. */
  networkRecoveryInterval: FiniteDuration) {

  requestedHeartbeat.foreach(duration =>
    require(duration.toSeconds > 0,
      "requestedHeartbeat < 1 second"))

  require(!connectionTimeout.isFinite || connectionTimeout.toMillis > 0,
    "connectionTimeout < 1 millisecond")
}