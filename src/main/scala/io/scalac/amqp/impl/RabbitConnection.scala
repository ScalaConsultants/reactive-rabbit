package io.scalac.amqp.impl

import com.rabbitmq.client.Connection
import io.scalac.amqp.AmqpConnection

private[amqp] class RabbitConnection(underlying: Connection) extends AmqpConnection {
  override def consume(queue: String) =
    new QueuePublisher(underlying, queue)

  override def publish(exchange: String, routingKey: String) =
    new ExchangeSubscriber(underlying.createChannel(), exchange, routingKey)
}