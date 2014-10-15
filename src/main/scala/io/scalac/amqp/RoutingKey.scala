package io.scalac.amqp


/** The routing key is used for routing messages depending on the exchange configuration. */
final case class RoutingKey(key: String) {
  require(key.length <= 255, "key.length > 255")
}
