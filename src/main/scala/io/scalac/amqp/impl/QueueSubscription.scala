package io.scalac.amqp.impl

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import com.rabbitmq.client._

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscription, Subscriber}


private[amqp] class QueueSubscription(channel: Channel, queue: String, subscriber: Subscriber[_ >: Delivery])
  extends DefaultConsumer(channel) with Subscription {
  val tag = UUID.randomUUID().toString
  val demand = new AtomicLong()

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

  def prefetch(demand: Long): Unit = demand match {
    case demand if demand < Int.MaxValue ⇒ channel.basicQos(demand.toInt, true)
    case Long.MaxValue                   ⇒ channel.basicQos(0, true) // 3.17: effectively unbounded
    case _                               ⇒ channel.basicQos(Int.MaxValue, true)
  }

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]) = {
    val delivery = Conversions.toDelivery(envelope, properties, body)
    demand.decrementAndGet() match {
      case 0      ⇒ getChannel.basicCancel(tag)
      case demand ⇒ prefetch(demand)
    }
    subscriber.onNext(delivery)
    getChannel.basicAck(envelope.getDeliveryTag, false)
  }

  override def request(n: Long) =
    channel.isOpen() match {
      case true ⇒
        require(n > 0, "Rule 3.9: n <= 0")
        try {
          demand.addAndGet(n) match {
            case demand if demand == n ⇒
              prefetch(demand)
              channel.basicConsume(queue, false, tag, this)
            case _                     ⇒ // demand increased
          }
        } catch {
          case exception: Exception ⇒
            subscriber.onError(exception)
        }
      case _     ⇒ // 3.6: nop
    }

  override def cancel() = try {
    channel.close()
  } catch {
    case _: AlreadyClosedException ⇒ // 3.7: nop
    case exception: Exception      ⇒
      subscriber.onError(exception)
  }
}
