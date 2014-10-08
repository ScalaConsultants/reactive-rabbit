package io.scalac.amqp


object Exchange {
  /** The default exchange is a direct exchange with no name (empty string) pre-declared by the broker.
    * It has one special property that makes it very useful for simple applications: every queue that is created
    * is automatically bound to it with a routing key which is the same as the queue name. */
  val Default = apply(
    name = "",
    `type` = Direct,
    durable = true,
    autoDelete = false,
    arguments = Map())
}

case class Exchange(name: String,
                    `type`: Type,
                    durable: Boolean,
                    autoDelete: Boolean,
                    arguments: Map[String, String])
