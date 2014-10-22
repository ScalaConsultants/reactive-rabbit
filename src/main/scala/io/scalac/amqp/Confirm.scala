package io.scalac.amqp

sealed trait Confirm

final case class Ack(tag: DeliveryTag) extends Confirm

final case class Reject(tag: DeliveryTag) extends Confirm

final case class Requeue(tag: DeliveryTag) extends Confirm