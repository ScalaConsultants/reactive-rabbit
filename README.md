Reactive Streams: AMQP
====

Experimental implementation of [Reactive Streams](www.reactive-streams.org) Reactive Streams for AMQP.

Example
----
```Scala
// streaming invoices to Accounting Department
val cf = new ConnectionFactory()
val connection = AmqpConnection(cf)
val publisher = connection.consume(queue = "invoices")
val subscriber = connection.publish(exchange = "accounting_department",
  routingKey = "invoices")

publisher.subscribe(new Subscriber[Delivery] {
  override def onError(t: Throwable) = subscriber.onError(t)
  override def onSubscribe(s: Subscription) = subscriber.onSubscribe(s)
  override def onComplete() = subscriber.onComplete()
  override def onNext(t: Delivery) = subscriber.onNext(t.message)
})
```
