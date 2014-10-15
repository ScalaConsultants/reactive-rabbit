package io.scalac.amqp.impl

import com.rabbitmq.client.Connection

import io.scalac.amqp.Delivery

import org.reactivestreams.{Subscriber, Publisher}


private[amqp] class QueuePublisher(connection: Connection, queue: String) extends Publisher[Delivery] {
  override def subscribe(subscriber: Subscriber[_ >: Delivery]) = try {
    val channel = connection.createChannel()
    val subscription = new QueueSubscription(channel, queue, subscriber)
    subscriber.onSubscribe(subscription)
  } catch {
    case exception: Exception ⇒ subscriber.onError(exception)
  }
}
