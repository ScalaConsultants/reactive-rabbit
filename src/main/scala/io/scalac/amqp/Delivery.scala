package io.scalac.amqp


final case class Delivery(
  /** Delivered message. */
  message: Message,

  /** Delivery tag. */
  deliveryTag: DeliveryTag,

  /** The exchange used for the current operation. */
  exchange: String,

  /** The routing key is used for routing messages depending on the exchange configuration. */
  routingKey: String,

  /** This is a hint as to whether this message may have been delivered before
    * (but not acknowledged). If the flag is not set, the message definitely has
    * not been delivered before. If it is set, it may have been delivered before. */
  redeliver: Boolean) {

  require(exchange.length <= 255, "exchange.length > 255")
  require(routingKey.length <= 255, "routingKey.length > 255")
}
