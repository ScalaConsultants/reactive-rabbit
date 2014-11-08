package io.scalac.amqp


object Exchange {
  /** Notification received after exchange is declared. */
  final case class DeclareOk()

  /** Notification received after exchange is deleted. */
  final case class DeleteOk()

  /** Notification received after exchange to exchange binding is added. */
  final case class BindOk()

  /** Notification received after exchange to exchange binding is removed. */
  final case class UnbindOk()
}

/** Exchanges are AMQP entities where messages are sent.
  * Exchanges take a message and route it into zero or more queues. */
final case class Exchange(
  name: String,

  /** The routing algorithm used depends on
    * the exchange type and rules called bindings. */
  `type`: Type,

  /** If set to true than exchange will survive broker restart. */
  durable: Boolean,

  /** If set, the exchange may not be used directly by publishers, but only when bound to other exchanges.
    * Internal exchanges are used to construct wiring that is not visible to applications. */
  internal: Boolean = false,

  /** Exchange is deleted when all queues have finished using it. */
  autoDelete: Boolean = false,

  /** Whenever an exchange with a configured AE cannot route a message to any queue, it publishes the message
    * to the specified AE instead. If that AE does not exist then a warning is logged. If an AE cannot route
    * a message, it in turn publishes the message to its AE, if it has one configured. This process continues
    * until either the message is successfully routed, the end of the chain of AEs is reached, or an AE
    * is encountered which has already attempted to route the message. */
  xAlternateExchange: Option[String] = None) {

  require(name.length <= 255, "name.length > 255")
}
