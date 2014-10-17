package io.scalac.amqp

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.collection.immutable.Seq
import scala.concurrent.duration._

import io.scalac.amqp.ConnectionSettings._


/** Hostname/port pair. */
final case class Address(
  /** The port to use when connecting to the broker. */
  host: String,

  /** The port to use when connecting to the broker. */
  port: Int) {

  require(port > 0 && port <= 65535,
    "port <= 0 || port > 65535")
}

object ConnectionSettings {
  /** Minimum value for connection timeout setting. */
  val TimeoutMin = 1.milli

  /** Maximum value for connection timeout setting. */
  val TimeoutMax = Int.MaxValue.millis

  /** Minimum value for heartbeat interval. */
  val HeartbeatMin = 1.second

  /** Maximum value for heartbeat interval. */
  val HeartbeatMax = Int.MaxValue.seconds

  /** Maximum value for network recovery interval setting. */
  val RecoveryIntervalMax = Int.MaxValue.millis


  /** Create settings with some sane default values.
    * Applicable only for connecting to RabbitMQ broker running on localhost. */
  def apply(): ConnectionSettings = apply(
    addresses = Seq(Address(host = "localhost", port = 5672)),
    virtualHost = "/",
    username = "guest",
    password = "guest",
    heartbeat = None,
    timeout = Duration.Inf,
    recoveryInterval = 5.seconds
  )

  def apply(config: Config): ConnectionSettings = apply(
    addresses = {
      import scala.collection.JavaConversions._
      config.getConfigList("amqp.addresses").map(address =>
        Address(
          host = address.getString("host"),
          port = address.getInt("port")
        ))(collection.breakOut)
    },
    virtualHost = config.getString("amqp.virtual-host"),
    username = config.getString("amqp.username"),
    password = config.getString("amqp.password"),
    heartbeat = config.getString("amqp.heartbeat").toLowerCase match {
      case "disable" ⇒ None
      case _         ⇒ Some(config.getMillisDuration("amqp.heartbeat"))
    },
    timeout = config.getString("amqp.timeout").toLowerCase match {
      case "infinite" ⇒ Duration.Inf
      case _          ⇒ config.getSecondsDuration("amqp.timeout")
    },
    recoveryInterval = config.getMillisDuration("amqp.recovery-interval")
  )

  /** INTERNAL API */
  private[amqp] final implicit class ConfigOps(val config: Config) extends AnyVal {
    def getMillisDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.MILLISECONDS)
    def getSecondsDuration(path: String): FiniteDuration = getDuration(path, TimeUnit.SECONDS)
    private def getDuration(path: String, unit: TimeUnit): FiniteDuration =
      Duration(config.getDuration(path, unit), unit)
  }
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
  heartbeat: Option[FiniteDuration],

  /** The default connection timeout, at least 1 millisecond. */
  timeout: Duration,

  /** How long will automatic recovery wait before attempting to reconnect. */
  recoveryInterval: FiniteDuration) {

  heartbeat.foreach(interval =>
    require(interval >= HeartbeatMin && interval <= HeartbeatMax,
      s"heartbeat < $HeartbeatMin || heartbeat > $HeartbeatMax"))

  require(!timeout.isFinite ||
    timeout >= TimeoutMin && timeout <= TimeoutMax,
    s"timeout < $TimeoutMin || timeout > $TimeoutMax")

  require(recoveryInterval <= RecoveryIntervalMax)
}