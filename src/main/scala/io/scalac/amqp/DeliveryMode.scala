package io.scalac.amqp


sealed trait DeliveryMode

/** A persistent message is held securely on disk and guaranteed to be delivered even
  * if there is a serious network failure, server crash, overflow etc.
  * Only works for queues that implement persistence. */
case object Persistent extends DeliveryMode

case object NonPersistent extends DeliveryMode
