package io.scalac.amqp

import io.scalac.amqp.impl.RabbitConnection

import org.reactivestreams.{Subscriber, Publisher}


object Connection {
  def apply(settings: ConnectionSettings) =
    new RabbitConnection(settings)
}

trait Connection {
  def consume(queue: String): Publisher[Delivery]

  def publish(exchange: String, routingKey: String): Subscriber[Message]
}
