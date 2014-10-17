package io.scalac.amqp


/** The server-assigned and channel-specific delivery tag assigned to each individual [[io.scalac.amqp.Delivery]].
  * The delivery tag is valid only within the subscription from which the message was received. */
final case class DeliveryTag(underlying: Long) extends AnyVal {
  def toLong = underlying
}
