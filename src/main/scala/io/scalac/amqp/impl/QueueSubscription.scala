package io.scalac.amqp.impl

import scala.collection.immutable.Queue
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.stm.{atomic, Ref}

import com.rabbitmq.client._
import com.google.common.primitives.Ints.saturatedCast

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscription, Subscriber}

import scala.util.control.NonFatal


private[amqp] class QueueSubscription(channel: Channel, subscriber: Subscriber[_ >: Delivery])
  extends DefaultConsumer(channel) with Subscription {

  val demand = Ref(0L)
  val buffer = Ref(Queue[Delivery]())

  override def handleCancel(consumerTag: String) = try {
    subscriber.onComplete()
  } catch {
    case exception: Exception ⇒
      subscriber.onError(new IllegalStateException("Rule 2.13 violation", exception))
  }

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = sig match {
    case sig if !sig.isInitiatedByApplication ⇒ subscriber.onError(sig)
    case _                                    ⇒ // shutdown initiated by us
  }

  def deliver(delivery: Delivery): Unit = {
    subscriber.onNext(delivery)
    channel.basicAck(delivery.deliveryTag.underlying, false)
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
          Queue[Delivery]()
        case d ⇒ // dequeue and decrease demand
          (buffer() :+ delivery).splitAt(saturatedCast(d)) match {
            case (head, tail) ⇒
              buffer() = tail
              demand -= head.size
              head
          }
      }
    }.foreach(deliver)
  }

  override def request(n: Long) =
    channel.isOpen() match {
      case true ⇒
        require(n > 0, "Rule 3.9: n <= 0")

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
                case (head, tail) ⇒
                  buffer() = tail
                  demand -= head.size
                  head
              }
          }
        }

        if (!buffered.isEmpty) {
          // 3.3: must continue somewhere else to prevent unbounded recursion
          Future(buffered.foreach(deliver))
        }
      case _    ⇒ // 3.6: nop
    }

  override def cancel() = try {
    channel.close()
  } catch {
    case _: AlreadyClosedException ⇒ // 3.7: nop
    case exception: Exception      ⇒
      subscriber.onError(exception)
  }
}
