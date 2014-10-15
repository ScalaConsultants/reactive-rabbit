Reactive Streams: AMQP
====

Experimental implementation of [Reactive Streams](http://www.reactive-streams.org) for AMQP based on [RabbitMQ](https://www.rabbitmq.com/) library.

Example
----
```Scala
// streaming invoices to Accounting Department
val factory = new ConnectionFactory()
val connection = AmqpConnection(factory)
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
