package io.scalac.amqp.impl

import com.google.common.primitives.Ints.saturatedCast
import com.rabbitmq.client._
import io.scalac.amqp.Delivery
import org.reactivestreams.{Subscriber, Subscription}

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{Ref, atomic}
import scala.util.control.NonFatal


private[amqp] class QueueSubscription(channel: Channel, queue: String, subscriber: Subscriber[_ >: Delivery])
  extends DefaultConsumer(channel) with Subscription {

  val demand = Ref(0L)

  /** Number of messages stored in this buffer is limited by channel QOS. */
  val buffer = Ref(Queue[Delivery]())
  val running = Ref(false)
  var closeRequested = Ref(false)

  override def finalize(): Unit = {
    try
      closeChannel()
    finally
      super.finalize()
  }

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



  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]) = {
    val delivery = Conversions.toDelivery(envelope, properties, body)
    buffer.single.transform(_ :+ delivery)
    deliverRequested()
  }

  @tailrec
  private def deliverRequested(): Unit = {
    val go = atomic { implicit txn ⇒
      if (demand() > 0 && buffer().nonEmpty)
        running.transformAndExtract(r => (true, !r))
      else
        false
    }

    if (go) {
      //buffer and demand could only grow since last check
      atomic { implicit txn ⇒
        buffer().splitAt(saturatedCast(demand())) match {
          case (ready, left) ⇒
            buffer() = left
            demand -= ready.size
            ready
        }
      }.foreach(deliver)

      running.single.set(false)
      deliverRequested()
    }
  }

  def deliver(delivery: Delivery): Unit = try {
    if(channel.isOpen()) {
      channel.basicAck(delivery.deliveryTag.underlying, false)
      subscriber.onNext(delivery)
    }
  } catch {
    case NonFatal(exception) ⇒
      // 2.13: exception from onNext cancels subscription
      try closeChannel() catch {
        case NonFatal(_) ⇒ // mute
      }
      subscriber.onError(exception)
  }

  def closeChannel(): Unit = synchronized {
    if (closeRequested.single.compareAndSet(false, true) && channel.isOpen) {
      try {
        channel.close()
      } catch {
        case NonFatal(_) =>
      }
    }
  }

  override def request(n: Long) = n match {
    case n if n <= 0         ⇒
      try closeChannel() catch {
        case NonFatal(_) ⇒ // mute
      }
      subscriber.onError(new IllegalArgumentException("Rule 3.9: n <= 0"))

    case n if channel.isOpen ⇒
      val newDemand = demand.single.transformAndGet(_ + n)
      newDemand match {
        case d if d < 0 ⇒ // 3.17: overflow
          try closeChannel() catch {
            case NonFatal(_) ⇒ // mute
          }
          subscriber.onError(new IllegalStateException("Rule 3.17: Pending + n > Long.MaxValue"))
        case d          ⇒
          Future(deliverRequested())
      }

    case _                   ⇒ // 3.6: nop
  }

  override def cancel() = try {
    closeChannel()
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
