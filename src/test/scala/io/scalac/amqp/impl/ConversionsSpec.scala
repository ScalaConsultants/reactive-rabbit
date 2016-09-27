package io.scalac.amqp.impl

import javax.net.ssl.SSLContext

import io.scalac.amqp.{Address, ConnectionSettings}
import org.scalatest.FlatSpec

import scala.concurrent.duration._
import scala.collection.immutable.Seq

class ConversionsSpec extends FlatSpec {
  behavior of "Conversions"

  it should "load a custom SSLContext" in {
    val context = SSLContext.getDefault
    val settings = ConnectionSettings(
      addresses         = Seq(Address(host = "localhost", port = 5672)),
      virtualHost       = "/",
      username          = "guest",
      password          = "guest",
      heartbeat         = None,
      timeout           = Duration.Inf,
      automaticRecovery = false,
      recoveryInterval  = 5.seconds,
      sslProtocol       = None,
      sslContext        = Some(context)
    )

    val connectionFactory = Conversions.toConnectionFactory(settings)

    assert(connectionFactory.isSSL)
  }
}
