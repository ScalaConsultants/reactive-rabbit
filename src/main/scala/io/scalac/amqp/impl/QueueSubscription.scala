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

  override def handleCancel(consumerTag: String) = subscriber.onComplete()

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = sig match {
    case sig if sig.isInitiatedByApplication => subscriber.onError(sig)
    case _ => // shutdown initiated by us
  }

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]) = {
    val delivery = Conversions.toDelivery(envelope, properties, body)
    demand.decrementAndGet() match {
      case 0 => getChannel.basicCancel(tag)
      case demand if demand > Int.MaxValue => getChannel.basicQos(Int.MaxValue)
      case demand => getChannel.basicQos(demand.toInt)
    }
    subscriber.onNext(delivery)
    getChannel.basicAck(envelope.getDeliveryTag, false)
  }

  override def request(n: Long) = {
    require(n >= 0, "n < 0")

    demand.addAndGet(n) match {
      case demand if demand == n =>
        if(demand > Int.MaxValue) {
          channel.basicQos(Int.MaxValue)
        } else {
          channel.basicQos(demand.toInt)
        }
        channel.basicConsume(queue, false, tag, this)
      case _ => // demand increased
    }
  }

  override def cancel() = channel.close()
}
