package io.scalac.amqp

import scala.collection.immutable.Seq
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

import org.scalatest.{FlatSpec, Matchers}


class ConnectionSettingsSpec extends FlatSpec with Matchers {
  "apply" should "load configuration" in {

    val config = ConfigFactory.load()
    val settings = ConnectionSettings(config)

    settings shouldBe ConnectionSettings(
      addresses = Seq(Address(host = "localhost", port = 5672)),
      virtualHost = "/",
      username = "guest",
      password = "guest",
      heartbeat = None,
      timeout = Duration.Inf,
      recoveryInterval = 5.seconds
    )
  }
}