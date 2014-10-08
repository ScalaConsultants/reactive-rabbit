package io.scalac.amqp


case class Delivery(/** Delivered message. */
                    message: Message,

                    /** Delivery tag. */
                    deliveryTag: DeliveryTag,

                    /** The exchange used for the current operation. */
                    exchange: String,

                    /** The associated routing key. */
                    routingKey: RoutingKey,

                    /** This is a hint as to whether this message may have been delivered before
                      * (but not acknowledged). If the flag is not set, the message definitely has
                      * not been delivered before. If it is set, it may have been delivered before. */
                    redeliver: Boolean)
