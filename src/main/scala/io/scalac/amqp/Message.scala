package io.scalac.amqp

import com.google.common.net.MediaType
import io.scalac.amqp.Message.{PriorityMax, PriorityMin}
import java.time.ZonedDateTime

import scala.concurrent.duration.Duration


object Message {
  val PriorityMin = 0
  val PriorityMax = 9
}


final case class Message(
  /** Message body. */
  body: IndexedSeq[Byte] = IndexedSeq.empty,

  /** The RFC-2046 MIME type for the messages application-data section (body).
    * Can contain a charset parameter defining the character encoding
    * used: e.g., ’text/plain; charset=“utf-8”’. Where the content type is unknown
    * the content-type SHOULD NOT be set, allowing the recipient to determine the actual type.
    * Where the section is known to be truly opaque binary data, the content-type SHOULD be set
    * to application/octet-stream. */
  contentType: Option[MediaType] = None,

  /** When present, describes additional content encodings applied to the application-data,
    * and thus what decoding mechanisms need to be applied in order to obtain the media-type
    * referenced by the content-type header field. Primarily used to allow a document to be
    * compressed without losing the identity of its underlying content type. A modifier to
    * the content-type, interpreted as per section 3.5 of RFC 2616. Valid content-encodings
    * are registered at IANA. Implementations SHOULD NOT use the compress encoding, except
    * as to remain compatible with messages originally sent with other protocols, e.g. HTTP or SMTP.
    * Implementations SHOULD NOT specify multiple content-encoding values except as to be
    * compatible with messages originally sent with other protocols, e.g. HTTP or SMTP. */
  contentEncoding: Option[String] = None,

  /** An application-specified list of header parameters and their values.
    * These may be setup for application-only use. Additionally, it is possible to create
    * queues with "Header Exchange Type" - when the queue is created, it is given a series
    * of header property names to match, each with optional values to be matched, so that
    * routing to this queue occurs via header-matching. */
  headers: Map[String, String] = Map(),

  /** Whether message should be persisted to disk.
    * Only works for queues that implement persistence. A persistent message is held securely
    * on disk and guaranteed to be delivered even if there is a serious network failure,
    * server crash, overflow etc. */
  mode: DeliveryMode = Persistent,

  /** The relative message priority (0 to 9).
    * A high priority message is sent ahead of lower priority messages waiting
    * in the same message queue. When messages must be discarded in order to maintain
    * a specific service quality level the server will first discard low-priority messages.
    * Only works for queues that implement priorities. */
  priority: Option[Int] = None,

  /** Message correlated to this one, e.g. what request this message is a reply to.
    * Applications are encouraged to use this attribute instead of putting this information
    * into the message payload. */
  correlationId: Option[String] = None,

  /** Queue name other apps should send the response to.
    * Commonly used to name a reply queue (or any other identifier that helps a consumer
    * application to direct its response). Applications are encouraged to use this attribute
    * instead of putting this information into the message payload. */
  replyTo: Option[String] = None,

  /** Expiration time after which the message will be deleted.
    * The value of the expiration field describes the TTL period in milliseconds.
    * When both a per-queue and a per-message TTL are specified, the lower value between the two will be chosen. */
  expiration: Duration = Duration.Inf,

  /** Message identifier as a string. If applications need to identify messages,
    * it is recommended that they use this attribute instead of putting it into the message payload. */
  messageId: Option[String] = None,

  /** Timestamp of the moment when message was sent. */
  timestamp: Option[ZonedDateTime] = None,

  /** Message type, e.g. what type of event or command this message represents.
    * Recommended to be used by applications instead of including this information
    * into the message payload. */
  `type`: Option[String] = None,

  /** Optional user ID. Verified by RabbitMQ against the actual connection username. */
  userId: Option[String] = None,

  /** Identifier of the application that produced the message. */
  appId: Option[String] = None) {

  priority.foreach(priority ⇒
    require(priority >= PriorityMin && priority <= PriorityMax,
      s"priority < $PriorityMin || priority > $PriorityMax"))
}
