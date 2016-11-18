Reactive Streams: AMQP
====

[![Join the chat at https://gitter.im/ScalaConsultants/reactive-rabbit](https://badges.gitter.im/ScalaConsultants/reactive-rabbit.svg)](https://gitter.im/ScalaConsultants/reactive-rabbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/ScalaConsultants/reactive-rabbit.svg?branch=master)](https://travis-ci.org/ScalaConsultants/reactive-rabbit)

[Reactive Streams](http://www.reactive-streams.org) driver for AMQP protocol. Powered by [RabbitMQ](https://www.rabbitmq.com/) library.

Available at Maven Central for Scala 2.11 and 2.12:

    libraryDependencies += "io.scalac" %% "reactive-rabbit" % "1.1.4"

Example
----

#### Akka Streams - 2.4.12

```Scala
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.scalac.amqp.Connection


// streaming invoices to Accounting Department
val connection = Connection()
// create org.reactivestreams.Publisher
val queue = connection.consume(queue = "invoices")
// create org.reactivestreams.Subscriber
val exchange = connection.publish(exchange = "accounting_department",
  routingKey = "invoices")

implicit val system = ActorSystem()
implicit val mat = ActorMaterializer()
// Run akka-streams with queue as Source and exchange as Sink
Source.fromPublisher(queue).map(_.message).runWith(Sink.fromSubscriber(exchange))
```

API Docs
----
Run `sbt doc` and open target/scala-2.12/index.html.

Settings
----
There are 3 options for passing AMQP settings:
* Use default settings from _reference.conf_ and _application.conf_. See [Config](https://github.com/typesafehub/config) library. See [refrence.conf](https://github.com/ScalaConsultants/reactive-rabbit/blob/master/src/main/resources/reference.conf) for settings layout.
```Scala
Connection()
```
* Use `Config` programatically.
```Scala
Connection(config : Config)
```
* Use `ConnectionSettings` programatically
```Scala
Connection(settings: ConnectionSettings)
```  

`ConnectionSettings` have following properties:
* `addresses: Seq[Address]` broker addresses (hostname/port pairs) to try in order. A random one will be picked during recovery.
* `virtualHost: String` virtual host to use when connecting to the broker.
* `username: String` user name to use when connecting to the broker.
* `password: String` password to use when connecting to the broker.
* `heartbeat: Option[FiniteDuration]` requested heartbeat interval, at least 1 second.`None` to disable heartbeat.
* `timeout: Duration` the default connection timeout, at least 1 millisecond.
* `automaticRecovery: Boolean` enable automatic connection recovery. Subscriptions are not recovered.
* `recoveryInterval: FiniteDuration` how long will automatic recovery wait before attempting to reconnect.
* `ssl: Option[String]` allows to use SSL for connecting to the broker. Valid values depend on JRE, [see  possiblities](http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext). Recent RabbitMQ servers does not allow SSL3.

Connection  
----
Connection trait API has two groups of methods: to manage AMQP infrastructure (ie. declare and delete exchanges, queues and bindings) and to create ReactiveStreams entities: `Publisher` and `Subscriber`.  
`consume(queue, prefetch)` - creates `Delivery` stream `Publisher` for messages from `queue`.  
`publish(exchange, routingKey)` - creates `Subscription` that takes stream of `Message` that will be sent to `exchange` with fixed `routingKey`.  
`publish(exchange)` - creates `Subscription` for stream of `Routed` (tuple of `Message` and routing key).  

