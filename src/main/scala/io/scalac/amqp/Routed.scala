package io.scalac.amqp

/** Routing key envelope for a [[Message]] */
final case class Routed(routingKey: String, message: Message)
