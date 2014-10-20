package io.scalac.amqp.impl

import java.util.UUID

import com.rabbitmq.client.AMQP

import io.scalac.amqp.{Connection, Delivery, Queue}

import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.reactivestreams.Publisher
import org.scalatest.testng.TestNGSuiteLike


/** You need to have RabbitMQ server to run these tests.
  * Here you can get it: https://www.rabbitmq.com/download.html */
class QueuePublisherSpec(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[Delivery](env, publisherShutdownTimeout) with TestNGSuiteLike {

  def this() = this(new TestEnvironment(600L), 1000L)

  val props = new AMQP.BasicProperties.Builder().build()
  val connection = Connection()
  val channel = connection.asInstanceOf[RabbitConnection].underlying.createChannel()

  override def createPublisher(elements: Long): Publisher[Delivery] = {
    val name = UUID.randomUUID().toString
    connection.declare(Queue(name = name, exclusive = true))
    1L.to(elements).foreach(_ ⇒ channel.basicPublish("", name, props, Array[Byte]()))
    connection.consume(name)
  }

  override def createErrorStatePublisher(): Publisher[Delivery] = {
    val conn = Connection()
    conn.asInstanceOf[RabbitConnection].underlying.close()
    conn.consume("whatever")
  }

  override def createPublisher1MustProduceAStreamOfExactly1Element() = ()
  override def createPublisher3MustProduceAStreamOfExactly3Elements() = ()
  override def spec102_maySignalLessThanRequestedAndTerminateSubscription() = ()
  override def spec103_mustSignalOnMethodsSequentially() = ()
  override def spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() = ()
  override def spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() = ()
  override def spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() = ()
  override def spec317_mustSupportAPendingElementCountUpToLongMaxValue() = ()
}