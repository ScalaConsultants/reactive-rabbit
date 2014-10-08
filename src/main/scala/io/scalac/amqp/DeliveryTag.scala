package io.scalac.amqp


final case class DeliveryTag(underlying: Long) extends AnyVal {
  def toLong = underlying
}
