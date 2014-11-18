package io.scalac.amqp.impl

import scala.concurrent.duration._

import io.scalac.amqp.{Connection, Routed}

import org.reactivestreams.tck.{TestEnvironment, SubscriberBlackboxVerification}
import org.scalatest.testng.TestNGSuiteLike


class ExchangeSubscriberBlackboxSpec(defaultTimeout: FiniteDuration) extends SubscriberBlackboxVerification[Routed](
  new TestEnvironment(defaultTimeout.toMillis)) with TestNGSuiteLike {

  def this() = this(300.millis)

  val connection = Connection()

  override def createSubscriber() =
    connection.publish("nowhere")

  override def createHelperPublisher(elements: Long) = ???

  override def spec201_blackbox_mustSignalDemandViaSubscriptionRequest() = notVerified()
  override def spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() = notVerified()
  override def spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() = notVerified()
  override def spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() = notVerified()
}
