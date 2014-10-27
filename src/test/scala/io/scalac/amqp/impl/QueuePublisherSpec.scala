package io.scalac.amqp.impl

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import com.rabbitmq.client.AMQP

import io.scalac.amqp.{Connection, Delivery, Queue}

import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.reactivestreams.{Subscription, Subscriber, Publisher}
import org.scalatest.testng.TestNGSuiteLike


/** You need to have RabbitMQ server to run these tests.
  * Here you can get it: https://www.rabbitmq.com/download.html */
class QueuePublisherSpec(env: TestEnvironment, publisherShutdownTimeout: Long)
  extends PublisherVerification[Delivery](env, publisherShutdownTimeout) with TestNGSuiteLike {


  def callAfterN(delegate: Publisher[Delivery], n: Long)(f: () ⇒ Unit) = new Publisher[Delivery] {
    require(n > 0)

    override def subscribe(subscriber: Subscriber[_ >: Delivery]) =
      delegate.subscribe(new Subscriber[Delivery] {
        val counter = new AtomicLong()

        override def onError(t: Throwable) = subscriber.onError(t)
        override def onSubscribe(s: Subscription) = subscriber.onSubscribe(s)
        override def onComplete() = subscriber.onComplete()

        override def onNext(t: Delivery) = {
          subscriber.onNext(t)

          counter.incrementAndGet() match {
            case `n` ⇒ f()
            case _   ⇒ // maybe next time
          }
        }
      })
  }

  def this() = this(new TestEnvironment(600L), 1000L)

  val props = new AMQP.BasicProperties.Builder().build()
  val connection = Connection()
  val channel = connection.asInstanceOf[RabbitConnection].underlying.createChannel()

  def declareQueueWithRandomName() = {
    val name = UUID.randomUUID().toString
    connection.declare(Queue(name = name, exclusive = true))
    name
  }

  override def createPublisher(elements: Long): Publisher[Delivery] = {
    val name = declareQueueWithRandomName()
    1L.to(elements).foreach(_ ⇒ channel.basicPublish("", name, props, Array[Byte]()))

    callAfterN(
      delegate = connection.consume(name),
      n = elements)(() ⇒
      connection.deleteQueue(name))
  }

  override def createErrorStatePublisher(): Publisher[Delivery] = {
    val conn = Connection()
    conn.asInstanceOf[RabbitConnection].underlying.close()
    conn.consume("whatever")
  }

  override def spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() = {
    val name = declareQueueWithRandomName()
    val publisher = connection.consume(name)

    val subscriber = new Subscriber[Delivery] {
      override def onSubscribe(subscription: Subscription) =
        subscription.request(Long.MaxValue)
      override def onNext(delivery: Delivery) = ()
      override def onError(t: Throwable) = ()
      override def onComplete() = ()
    }

    publisher.subscribe(subscriber)

    intercept[IllegalStateException] {
      publisher.subscribe(subscriber)
    }
  }
}