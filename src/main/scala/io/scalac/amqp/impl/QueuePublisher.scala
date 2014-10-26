package io.scalac.amqp.impl

import com.rabbitmq.client.Connection

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscriber, Publisher}


private[amqp] class QueuePublisher(connection: Connection, queue: String, prefetch: Int = 20)
  extends Publisher[Delivery] {
  override def subscribe(subscriber: Subscriber[_ >: Delivery]) = try {
    val channel = connection.createChannel()

    val subscription = new QueueSubscription(channel, subscriber)
    subscriber.onSubscribe(subscription)

    channel.basicQos(prefetch)
    channel.basicConsume(queue, false, subscription)
  } catch {
    case exception: Exception ⇒ subscriber.onError(exception)
  }
}
