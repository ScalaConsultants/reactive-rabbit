package io.scalac.amqp


case class Queue(name: String,
                 durable: Boolean = true,
                 exclusive: Boolean = false,
                 autoDelete: Boolean = false,
                 arguments: Map[String, String] = Map())
