package io.scalac.amqp

import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

import com.typesafe.config.Config
import io.scalac.amqp.ConnectionSettings._

import scala.collection.immutable.Seq
import scala.concurrent.duration._


/** Hostname/port pair. */
final case class Address(
  /** The port to use when connecting to the broker. */
  host: String,

  /** The port to use when connecting to the broker. */
  port: Int) {

  require(port > 0 && port <= 65535,
    "port <= 0 || port > 65535")

  override def toString = s"Address(host=$host, port=$port)"
}

object ConnectionSettings {
  /** SSL protocol e.g. "TLSv1" or "TLSv1.2". */
  type Protocol = String

  /** Minimum value for connection timeout setting. */
  val TimeoutMin = 1.milli

  /** Maximum value for connection timeout setting. */
  val TimeoutMax = Int.MaxValue.millis

  /** Minimum value for heartbeat interval. */
  val HeartbeatMin = 1.second

  /** Maximum value for heartbeat interval. */
  val HeartbeatMax = Int.MaxValue.seconds

  /** Builds settings from TypeSafe Config. */
  def apply(config: Config): ConnectionSettings = apply(
    addresses = {
      import scala.collection.JavaConverters._
      config.getConfigList("amqp.addresses").asScala.map(address ⇒
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
      case _         ⇒ Some(config.getSecondsDuration("amqp.heartbeat"))
    },
    timeout = config.getString("amqp.timeout").toLowerCase match {
      case "infinite" ⇒ Duration.Inf
      case _          ⇒ config.getMillisDuration("amqp.timeout")
    },
    automaticRecovery = config.getBoolean("amqp.automatic-recovery"),
    recoveryInterval = config.getMillisDuration("amqp.recovery-interval"),
    sslProtocol = config.getString("amqp.ssl") match {
      case "disable" ⇒ None
      case protocol  ⇒ Some(protocol)
    }
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

  /** Enable automatic connection recovery. Subscriptions are not recovered. */
  automaticRecovery: Boolean,

  /** How long will automatic recovery wait before attempting to reconnect. */
  recoveryInterval: FiniteDuration,

  /** Allows to use SSL for connecting to the broker.
    * Pass in the SSL protocol to use, e.g. "TLSv1" or "TLSv1.2" or none. */
  sslProtocol: Option[Protocol],

  /** Allows for use of a custom SSL Context when connecting to the broker. */
  sslContext: Option[SSLContext] = None) {

  heartbeat.foreach(interval ⇒
    require(interval >= HeartbeatMin && interval <= HeartbeatMax,
      s"heartbeat < $HeartbeatMin || heartbeat > $HeartbeatMax"))

  require(!timeout.isFinite ||
    timeout >= TimeoutMin && timeout <= TimeoutMax,
    s"timeout < $TimeoutMin || timeout > $TimeoutMax")

  /** Returns a string representation of this. Password field is intentionally omitted. */
  override def toString = s"ConnectionSettings(addresses=$addresses, virtualHost=$virtualHost, username=$username, " +
    s"heartbeat=$heartbeat, timeout=$timeout, recoveryInterval=$recoveryInterval, ssl=$sslProtocol)"
}