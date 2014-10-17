package io.scalac.amqp


/** Queues store and forward messages. Queues can be configured in the server or created at runtime.
  * Queues must be attached to at least one exchange in order to receive messages from publishers. */
final case class Queue(
  /** Name for the queue. */
  name: String,

  /** Durable queues are persisted to disk and thus survive broker restarts.
    * Queues that are not durable are called transient. Not all scenarios
    * and use cases mandate queues to be durable.
    *
    * Durability of a queue does not make messages that are routed to that queue durable.
    * If broker is taken down and then brought back up, durable queue will be re-declared
    * during broker startup, however, only persistent messages will be recovered. */
  durable: Boolean = true,

  /** Used by only one connection and the queue will be deleted when that connection closes. */
  exclusive: Boolean = false,

  /** Queue is deleted when last consumer unsubscribes but not before first one connects. */
  autoDelete: Boolean = false,

  /** Some brokers use it to implement additional features like message TTL. */
  arguments: Map[String, String] = Map()) {

  require(name.length <= 255, "name.length > 255")
}
