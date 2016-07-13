package io.scalac.amqp.impl

import com.rabbitmq.client.{Connection, ShutdownListener, ShutdownSignalException}
import io.scalac.amqp.Delivery
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.stm.Ref
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


private[amqp] class QueuePublisher(
  /** RabbitMQ library connection. */
  connection: Connection,

  /** Queue to consume from. */
  queue: String,

  /** Number of unacknowledged messages in the flight. It's beneficial to have this number higher
    * than 1 due to improved throughput. Setting this number to high may increase memory usage -
    * depending on average message size and speed of subscribers. */
  prefetch: Int = 20,

  /**
    * If set to true, the consumer will be exclusive and only this consumer can access the queue.
    */
  exclusive: Boolean = false) extends Publisher[Delivery] {

  require(prefetch > 0, "prefetch <= 0")

  val subscribers = Ref(Set[Subscriber[_ >: Delivery]]())

  override def subscribe(subscriber: Subscriber[_ >: Delivery]) =
    subscribers.single.getAndTransform(_ + subscriber) match {
      case ss if ss.contains(subscriber) ⇒
        throw new IllegalStateException(s"Rule 1.10: Subscriber=$subscriber is already subscribed to this publisher.")
      case _ ⇒
        Try(connection.createChannel()) match {
          case Success(channel) ⇒
            val subscription = new QueueSubscription(channel, queue, subscriber)
            subscription.addShutdownListener(newShutdownListener(subscriber))

            try {
              subscriber.onSubscribe(subscription)
              channel.basicQos(prefetch)
              channel.basicConsume(queue, false, "", false, exclusive, null, subscription)
            } catch {
              case NonFatal(exception) ⇒ subscriber.onError(exception)
            }

          case Failure(cause) ⇒
            subscriber.onSubscribe(CanceledSubscription)
            subscriber.onError(cause)
        }
    }

  def newShutdownListener(subscriber: Subscriber[_ >: Delivery]) =
    () => subscribers.single.transform(_ - subscriber)

  override def toString = s"QueuePublisher(connection=$connection, queue=$queue, prefetch=$prefetch)"
}
