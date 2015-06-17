package io.scalac.amqp.impl

import scala.collection.immutable.Queue
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.stm.{atomic, Ref}
import scala.util.control.NonFatal

import com.rabbitmq.client._
import com.google.common.primitives.Ints.saturatedCast

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscription, Subscriber}


private[amqp] class QueueSubscription(channel: Channel, queue: String, subscriber: Subscriber[_ >: Delivery])
  extends DefaultConsumer(channel) with Subscription {

  val demand = Ref(0L)

  /** Number of messages stored in this buffer is limited by channel QOS. */
  val buffer = Ref(Queue[Delivery]())

  /** to guarantee order of delivery */
  val futureQueue = Ref(Future.successful(Unit))

  override def handleCancel(consumerTag: String) = try {
    subscriber.onComplete()
  } catch {
    case NonFatal(exception) ⇒
      subscriber.onError(new IllegalStateException("Rule 2.13: onComplete threw an exception", exception))
  }

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = sig match {
    case sig if !sig.isInitiatedByApplication ⇒ subscriber.onError(sig)
    case _                                    ⇒ // shutdown initiated by us
  }

  def deliver(delivery: Delivery): Unit = try {
    if(channel.isOpen()) {
      channel.basicAck(delivery.deliveryTag.underlying, false)
      subscriber.onNext(delivery)
    }
  } catch {
    case NonFatal(exception) ⇒
      // 2.13: exception from onNext cancels subscription
      try (channel.close()) catch {
        case NonFatal(_) ⇒ // mute
      }
      subscriber.onError(exception)
  }

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]) = {
    val delivery = Conversions.toDelivery(envelope, properties, body)

    atomic { implicit txn ⇒
      demand() match {
        case 0 ⇒ // no demand, store for later
          buffer.transform(_ :+ delivery)
          Queue()
        case d ⇒ // dequeue and decrease demand
          (buffer() :+ delivery).splitAt(saturatedCast(d)) match {
            case (ready, left) ⇒
              buffer() = left
              demand -= ready.size
              ready
          }
      }
    }.foreach(deliver)
  }

  override def request(n: Long) = n match {
    case n if n <= 0           ⇒
      try (channel.close()) catch {
        case NonFatal(_) ⇒ // mute
      }
      subscriber.onError(new IllegalArgumentException("Rule 3.9: n <= 0"))

    case n if channel.isOpen() ⇒
      val buffered = atomic { implicit txn ⇒
        demand.transformAndGet(_ + n) match {
          case d if d < 0 ⇒ // 3.17: overflow
            try (channel.close()) catch {
              case NonFatal(_) ⇒ // mute
            }
            subscriber.onError(new IllegalStateException("Rule 3.17: Pending + n > Long.MaxValue"))
            Queue()
          case d          ⇒
            buffer().splitAt(saturatedCast(d)) match {
              case (ready, left) ⇒
                buffer() = left
                demand -= ready.size
                ready
            }
        }
      }

      if (!buffered.isEmpty) {
        // 3.3: must continue somewhere else to prevent unbounded recursion
        atomic { implicit txn ⇒
          futureQueue.getAndTransform {
            future => future.andThen { case _ ⇒ buffered.foreach(deliver) }
          }
        }
      }

    case _                     ⇒ // 3.6: nop
  }

  override def cancel() = try {
    channel.close()
  } catch {
    case _: AlreadyClosedException ⇒ // 3.7: nop
    case NonFatal(exception)       ⇒
      subscriber.onError(exception)
  }

  override def toString() = atomic { implicit txn ⇒
    s"QueueSubscription(channel=$channel, queue=$queue, subscriber=$subscriber, demand=${demand()}, " +
      s"buffer.size=${buffer().size})"
  }
}
