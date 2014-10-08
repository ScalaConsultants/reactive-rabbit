package io.scalac.amqp

import com.rabbitmq.client.ConnectionFactory

import io.scalac.amqp.impl.RabbitConnection

import org.reactivestreams.{Subscriber, Publisher}


object AmqpConnection {
  def apply(cf: ConnectionFactory) =
    new RabbitConnection(cf.newConnection())
}

trait AmqpConnection {
  def consume(queue: String): Publisher[Delivery]

  def publish(exchange: String, routingKey: String): Subscriber[Message]
}
