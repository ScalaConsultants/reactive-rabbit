package io.scalac.amqp


/** Exchanges are AMQP entities where messages are sent.
  * Exchanges take a message and route it into zero or more queues. */
final case class Exchange(
  name: String,

  /** The routing algorithm used depends on
    * the exchange type and rules called bindings. */
  `type`: Type,

  /** If set to true than exchange will survive broker restart. */
  durable: Boolean,

  /** Exchange is deleted when all queues have finished using it. */
  autoDelete: Boolean) {

  require(name.length <= 255, "name.length > 255")
}
