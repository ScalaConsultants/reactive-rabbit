package io.scalac.amqp


case class Exchange(name: String,
                    `type`: Type,
                    durable: Boolean,
                    autoDelete: Boolean,
                    arguments: Map[String, String])
