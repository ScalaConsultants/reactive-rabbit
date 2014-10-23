package io.scalac.amqp


final case class Routed(routingKey: String, message: Message)
