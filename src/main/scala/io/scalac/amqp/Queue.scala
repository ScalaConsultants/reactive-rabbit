package io.scalac.amqp

import io.scalac.amqp.Queue._

import scala.concurrent.duration._


object Queue {
  /** Notification received after queue is declared. */
  final case class DeclareOk(
    /** Name of declared queue. */
    queue: String,
    /** Number of messages stored in queue. */
    messageCount: Int,
    /** Number of connected consumers. */
    consumerCount: Int)

  /** Notification received after queue is deleted. */
  final case class DeleteOk(
    /** Number of messages removed together with the queue. */
    messageCount: Int)

  /** Notification received after queue binding is added. */
  final case class BindOk()

  /** Notification received after queue binding is removed. */
  final case class UnbindOk()

  /** Notification received after queue is purged. */
  final case class PurgeOk(
    /** Number of messages removed from the queue. */
    messageCount: Int)

  /** Setting the TTL to 0 causes messages to be expired upon reaching a queue unless
    * they can be delivered to a consumer immediately. Thus this provides an alternative
    * to immediate flag, which the RabbitMQ server does not support. */
  val XMessageTtlMin = 0L.millis
  val XMessageTtlMax = 4294967295L.millis

  val XExpiresMin = 0L.millis

  val XMaxLengthMin = 0L
  val XMaxLengthMax = 4294967295L

  val XMaxBytesMin = 0L
  val XMaxBytesMax = 4294967295L

  /** Dead letter exchanges (DLXs) are normal exchanges.
    * They can be any of the usual types and are declared as usual. */
  final case class XDeadLetterExchange(
    /** Name of dead letter exchange. */
    name: String,
    /** You may also specify a routing key to be used when dead-lettering messages.
      * If this is not set, the message's own routing keys will be used. */
    routingKey: Option[String]) {

    require(name.length <= 255, "name.length > 255")
    routingKey.foreach(routingKey ⇒
      require(routingKey.length <= 255, "routingKey.length > 255"))
  }
}


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
  durable: Boolean = false,

  /** Used by only one connection and the queue will be deleted when that connection closes. */
  exclusive: Boolean = false,

  /** Queue is deleted when last consumer unsubscribes but not before first one connects. */
  autoDelete: Boolean = false,

  /** A message that has been in the queue for longer than the configured TTL is said to be dead.
    * Note that a message routed to multiple queues can die at different times, or not at all,
    * in each queue in which it resides. The death of a message in one queue has no impact on
    * the life of the same message in other queues. Setting the TTL to 0 causes messages to
    * be expired upon reaching a queue unless they can be delivered to a consumer immediately.
    * Thus this provides an alternative to immediate flag, which the RabbitMQ server does not support.
    * When both a per-queue and a per-message TTL are specified, the lower value between the two will be chosen.
    *
    * Caveats:
    * While consumers never see expired messages, only when expired messages reach the head
    * of a queue will they actually be discarded (or dead-lettered). When setting a per-queue
    * TTL this is not a problem, since expired messages are always at the head of the queue.
    * When setting per-message TTL however, expired messages can queue up behind non-expired ones
    * until the latter are consumed or expired. Hence resources used by such expired messages will
    * not be freed, and they will be counted in queue statistics (e.g. the number of messages in the queue). */
  xMessageTtl: Duration = Duration.Inf,

  /** This controls for how long a queue can be unused before it is automatically deleted.
    * Unused means the queue has no consumers, the queue has not been redeclared, and basic.get
    * has not been invoked for a duration of at least the expiration period. This can be used,
    * for example, for RPC-style reply queues, where many queues can be created which may never be drained. */
  xExpires: Duration = Duration.Inf,

  /** Maximum length can be set by supplying value for this field. Queue length is a measure that takes
    * into account ready messages, ignoring unacknowledged messages and message size. Messages will be dropped
    * or dead-lettered from the front of the queue to make room for new messages once the limit is reached. */
  xMaxLength: Option[Long] = None,

  /** Messages from a queue can be 'dead-lettered'; that is, republished to another exchange
    * when any of the following events occur:
    *
    * - The message is rejected (not requeued),
    * - The TTL for the message expires,
    * - The queue length limit is exceeded.
    *
    * Dead letter exchanges (DLXs) are normal exchanges.
    * They can be any of the usual types and are declared as usual. */
  xDeadLetterExchange: Option[XDeadLetterExchange] = None,

  /** Maximum queue size in bytes can be set by supplying a value for this field. If both xMaxBytes
    * and xMaxLength are provided, whichever limit is hit first will be enforced. */
  xMaxBytes: Option[Long] = None) {

  require(name.length <= 255, "name.length > 255")
  require(!xMessageTtl.isFinite ||
    xMessageTtl >= XMessageTtlMin && xMessageTtl <= XMessageTtlMax,
    s"xMessageTtl < $XMessageTtlMin || xMessageTtl > $XMessageTtlMax")
  require(!xExpires.isFinite || xExpires >= XExpiresMin,
    s"xExpires < $XExpiresMin")
  xMaxLength.foreach(xMaxLength ⇒
    require(xMaxLength >= XMaxLengthMin && xMaxLength <= XMaxLengthMax,
      s"xMaxLength < $XMaxLengthMin || xMaxLength > $XMaxLengthMax"))
  xMaxBytes.foreach(xMaxBytes ⇒
    require(xMaxBytes >= XMaxBytesMin && xMaxBytes <= XMaxBytesMax,
      s"xMaxBytes < $XMaxBytesMin || xMaxBytes > $XMaxBytesMax"))
}
