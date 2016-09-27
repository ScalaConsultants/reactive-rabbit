package io.scalac.amqp

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.Seq
import scala.concurrent.duration._

class ConnectionSettingsSpec extends FlatSpec with Matchers {

  val referenceSettings =
    ConnectionSettings(
      addresses         = Seq(Address(host = "localhost", port = 5672)),
      virtualHost       = "/",
      username          = "guest",
      password          = "guest",
      heartbeat         = None,
      timeout           = Duration.Inf,
      automaticRecovery = false,
      recoveryInterval  = 5.seconds,
      sslProtocol       = None)

  "apply" should "be able to load configuration from TypeSafe Config" in {
    val settings = ConnectionSettings(ConfigFactory.load("application.conf"))
    if(settings.addresses.head.host == "localhost")
      settings shouldBe referenceSettings
     else settings shouldBe referenceSettings.copy(addresses = referenceSettings.addresses.map(_.copy(host = "boot2docker")))

    def parseAndLoad(s: String) = ConfigFactory.load(ConfigFactory.parseString(s))

    ConnectionSettings(parseAndLoad("amqp.heartbeat = 5 seconds")).heartbeat shouldBe Some(5.seconds)
    ConnectionSettings(parseAndLoad("amqp.timeout = 10 seconds")).timeout shouldBe 10.seconds
  }
}