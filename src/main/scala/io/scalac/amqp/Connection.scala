package io.scalac.amqp

import java.io.IOException

import com.typesafe.config.{ConfigFactory, Config}

import io.scalac.amqp.impl.RabbitConnection

import org.reactivestreams.{Subscriber, Publisher}


object Connection {
  def apply(): Connection =
    apply(ConfigFactory.load())

  def apply(config: Config): Connection =
    apply(ConnectionSettings(config))

  def apply(settings: ConnectionSettings): Connection =
    new RabbitConnection(settings)
}


trait Connection {
  /** Declare an exchange. */
  @throws[IOException]
  def declare(exchange: Exchange): Unit

  /** Declare a queue. */
  @throws[IOException]
  def declare(queue: Queue): Unit

  /** Delete a queue, without regard for whether it is in use or has messages on it. */
  @throws[IOException]
  def deleteQueue(name: String): Unit

  /** Delete an exchange, without regard for whether it is in use or not. */
  @throws[IOException]
  def deleteExchange(name: String): Unit

  def consume(queue: String): Publisher[Delivery]

  def publish(exchange: String, routingKey: String): Subscriber[Message]

  def publish(exchange: String): Subscriber[Routed]

  def publishDirectly(queue: String): Subscriber[Message]
}
