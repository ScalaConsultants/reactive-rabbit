package io.scalac.amqp

import scala.collection.immutable.Seq
import scala.concurrent.duration._

import com.typesafe.config.ConfigFactory

import org.scalatest.{FlatSpec, Matchers}


class ConnectionSettingsSpec extends FlatSpec with Matchers {
  "apply" should "be able to load configuration from TypeSafe Config" in {
    val settings = ConnectionSettings(ConfigFactory.load())
    settings shouldBe ConnectionSettings(
      addresses = Seq(Address(host = "localhost", port = 5672)),
      virtualHost = "/",
      username = "guest",
      password = "guest",
      heartbeat = None,
      timeout = Duration.Inf,
      recoveryInterval = 5.seconds
    )

    def parseAndLoad(s: String) = ConfigFactory.load(ConfigFactory.parseString(s))

    ConnectionSettings(parseAndLoad("amqp.heartbeat = 5 seconds")).heartbeat shouldBe Some(5.seconds)
    ConnectionSettings(parseAndLoad("amqp.timeout = 10 seconds")).timeout shouldBe 10.seconds
  }
}