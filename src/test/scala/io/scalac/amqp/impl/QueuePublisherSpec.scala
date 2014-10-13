package io.scalac.amqp.impl

import java.util.UUID

import com.google.common.collect.ImmutableMap
import com.rabbitmq.client.{AMQP, ConnectionFactory}

import io.scalac.amqp.Delivery

import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.reactivestreams.{Subscriber, Publisher}
import org.scalatest.testng.TestNGSuiteLike


/** You need to have RabbitMQ server to run these tests.
  * Here you can get it: https://www.rabbitmq.com/download.html */
class QueuePublisherSpec(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[Delivery](env, publisherShutdownTimeout) with TestNGSuiteLike {

  def this() = this(new TestEnvironment(600L), 1000L)

  val props = new AMQP.BasicProperties.Builder().build()
  val factory = new ConnectionFactory
  val connection = factory.newConnection()
  val channel = connection.createChannel()

  override def createPublisher(elements: Long): Publisher[Delivery] = {
    val queue = channel.queueDeclare(UUID.randomUUID().toString,
      false, false, false, ImmutableMap.of("x-expires", 30000.asInstanceOf[Object])).getQueue
    1L.to(elements).foreach(_ => channel.basicPublish("", queue, props, Array[Byte]()))
    new QueuePublisher(connection, queue)
  }

  override def createErrorStatePublisher(): Publisher[Delivery] = {
    val connection = factory.newConnection()
    connection.close()
    new QueuePublisher(connection, "foo")
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