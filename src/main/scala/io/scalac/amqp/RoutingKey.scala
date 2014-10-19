package io.scalac.amqp


/** The routing key is used for routing messages depending on the exchange configuration. */
final case class RoutingKey(value: String) {
  require(value.length <= 255, "value.length > 255")
}
