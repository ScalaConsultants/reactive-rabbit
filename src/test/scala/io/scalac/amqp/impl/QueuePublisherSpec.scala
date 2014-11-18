package io.scalac.amqp.impl

import scala.concurrent.Await
import scala.concurrent.duration._

import java.util.concurrent.atomic.AtomicLong

import com.rabbitmq.client.AMQP

import io.scalac.amqp.{Connection, Delivery, Queue}

import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.reactivestreams.{Subscription, Subscriber, Publisher}
import org.scalatest.testng.TestNGSuiteLike


/** You need to have RabbitMQ server to run these tests.
  * Here you can get it: https://www.rabbitmq.com/download.html */
class QueuePublisherSpec(defaultTimeout: FiniteDuration, publisherShutdownTimeout: FiniteDuration)
  extends PublisherVerification[Delivery](new TestEnvironment(defaultTimeout.toMillis),
    publisherShutdownTimeout.toMillis) with TestNGSuiteLike {

  def this() = this(600.millis, 1.second)

  /** Calls a function after passing n messages. */
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
            case _ ⇒ // maybe next time
          }
        }
      })
  }

  val props = new AMQP.BasicProperties.Builder().build()
  val connection = Connection()
  val channel = connection.asInstanceOf[RabbitConnection].underlying.createChannel()

  def declareQueue(): Queue = Await.result(connection.queueDeclare(), defaultTimeout)
  def deleteQueue(queue: String): Unit = Await.ready(connection.queueDelete(queue), defaultTimeout)


  /** Queues are not finite in general. To simulate finite queues we remove queue after passing N messages.
    * This also tests if [[QueueSubscription.handleCancel]] works as intended. */
  override def createPublisher(elements: Long): Publisher[Delivery] = {
    val queue = declareQueue()
    1L.to(elements).foreach(_ ⇒ channel.basicPublish("", queue.name, props, Array[Byte]()))

    callAfterN(
      delegate = connection.consume(queue.name),
      n = elements)(() ⇒ deleteQueue(queue.name))
  }

  override def createErrorStatePublisher(): Publisher[Delivery] = {
    val conn = Connection()
    conn.asInstanceOf[RabbitConnection].underlying.close()
    conn.consume("whatever")
  }

  override def spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() = {
    val queue = declareQueue()
    val publisher = connection.consume(queue.name)

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