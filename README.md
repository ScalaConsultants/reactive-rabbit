Reactive Streams: AMQP
====

[![Build Status](https://travis-ci.org/ScalaConsultants/reactive-rabbit.svg?branch=master)](https://travis-ci.org/ScalaConsultants/reactive-rabbit)

[Reactive Streams](http://www.reactive-streams.org) driver for AMQP protocol. Powered by [RabbitMQ](https://www.rabbitmq.com/) library.

Available at Maven Central:

    libraryDependencies += "io.scalac" % "reactive-rabbit_2.11" % "0.2.1"

Example
----

#### Akka Streams - 0.10

```Scala
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.scalac.amqp.Connection


// streaming invoices to Accounting Department
val connection = Connection()
val queue = connection.consume(queue = "invoices")
val exchange = connection.publish(exchange = "accounting_department",
  routingKey = "invoices")

implicit val system = ActorSystem()
implicit val materializer = FlowMaterializer()

Source(queue).map(_.message).to(Sink(exchange)).run()
```
