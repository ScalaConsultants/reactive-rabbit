package io.scalac.amqp.impl

import scala.concurrent.stm.Ref
import scala.util.control.NonFatal

import com.rabbitmq.client.{ShutdownSignalException, ShutdownListener, Connection}

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscriber, Publisher}


private[amqp] class QueuePublisher(
  /** RabbitMQ library connection. */
  connection: Connection,

  /** Queue to consume from. */
  queue: String,

  /** Number of unacknowledged messages in the flight. It's beneficial to have this number higher
    * than 1 due to improved throughput. Setting this number to high may increase memory usage -
    * depending on average message size and speed of subscribers. */
  prefetch: Int = 20) extends Publisher[Delivery] {

  require(prefetch > 0, "prefetch <= 0")

  val subscribers = Ref(Set[Subscriber[_ >: Delivery]]())

  override def subscribe(subscriber: Subscriber[_ >: Delivery]) =
    subscribers.single.getAndTransform(_ + subscriber) match {
      case ss if ss.contains(subscriber) ⇒
        throw new IllegalStateException(s"Rule 1.10: Subscriber=$subscriber is already subscribed to this publisher.")
      case _                             ⇒ try {
        val channel = connection.createChannel()
        channel.addShutdownListener(newShutdownListener(subscriber))

        val subscription = new QueueSubscription(channel, queue, subscriber)
        subscriber.onSubscribe(subscription)

        channel.basicQos(prefetch)
        channel.basicConsume(queue, false, subscription)
      } catch {
        case NonFatal(exception) ⇒ subscriber.onError(exception)
      }
    }

  def newShutdownListener(subscriber: Subscriber[_ >: Delivery]) = new ShutdownListener {
    override def shutdownCompleted(cause: ShutdownSignalException) =
      subscribers.single.transform(_ - subscriber)
  }

  override def toString = s"QueuePublisher(connection=$connection, queue=$queue, prefetch=$prefetch)"
}
