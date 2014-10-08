package io.scalac.amqp

/** The server-assigned message delivery tag. */
final case class DeliveryTag(underlying: Long) extends AnyVal {
  def toLong = underlying
}
