package io.scalac.amqp.impl

import scala.concurrent.stm.Ref

import com.rabbitmq.client.{ShutdownSignalException, ShutdownListener, Connection}

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscriber, Publisher}


private[amqp] class QueuePublisher(connection: Connection, queue: String, prefetch: Int = 20)
  extends Publisher[Delivery] {
  val subscribers = Ref(Set[Subscriber[_ >: Delivery]]())

  override def subscribe(subscriber: Subscriber[_ >: Delivery]) =
    subscribers.single.getAndTransform(_ + subscriber) match {
      case ss if ss.contains(subscriber) ⇒
        throw new IllegalStateException(s"1.10: Subscriber=$subscriber is already subscribed to this publisher.")
      case _                             ⇒ try {
        val channel = connection.createChannel()
        channel.addShutdownListener(newShutdownListener(subscriber))

        val subscription = new QueueSubscription(channel, subscriber)
        subscriber.onSubscribe(subscription)

        channel.basicQos(prefetch)
        channel.basicConsume(queue, false, subscription)
      } catch {
        case exception: Exception ⇒ subscriber.onError(exception)
      }
    }

  def newShutdownListener(subscriber: Subscriber[_ >: Delivery]) = new ShutdownListener {
    override def shutdownCompleted(cause: ShutdownSignalException) =
      subscribers.single.transform(_ - subscriber)
  }
}
