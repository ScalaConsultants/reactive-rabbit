Reactive Streams: AMQP
====

Experimental implementation of [Reactive Streams](http://www.reactive-streams.org) for AMQP based on [RabbitMQ](https://www.rabbitmq.com/) library.

Examples
----

#### Akka Streams (0.9)

```Scala
import akka.actor.ActorSystem
import akka.stream.scaladsl2.{FlowMaterializer, Sink, Source}
import io.scalac.amqp.Connection


// streaming invoices to Accounting Department
val connection = Connection()
val queue = connection.consume(queue = "invoices")
val exchange = connection.publish(exchange = "accounting_department",
  routingKey = "invoices")

implicit val system = ActorSystem()
implicit val materializer = FlowMaterializer()

Source(queue).map(_.message).connect(Sink(exchange)).run()
```

#### Reactive Streams

```Scala
import io.scalac.amqp.{Connection, Delivery}
import org.reactivestreams.{Subscriber, Subscription}


// streaming invoices to Accounting Department
val connection = Connection()
val queue = connection.consume(queue = "invoices")
val exchange = connection.publish(exchange = "accounting_department",
  routingKey = "invoices")

queue.subscribe(new Subscriber[Delivery] {
  override def onError(t: Throwable) = exchange.onError(t)
  override def onSubscribe(s: Subscription) = exchange.onSubscribe(s)
  override def onComplete() = exchange.onComplete()
  override def onNext(t: Delivery) = exchange.onNext(t.message)
})
```

Caveats
----
 * `QueueSubscription` will cancel underyling consumer when demand goes to zero. This will cause queue with `auto-delete` attribute to get deleted by the broker. Solution to this problem is not ready yet. Exclusive queues are not affected beucase their life cycle is bounded to connection.
