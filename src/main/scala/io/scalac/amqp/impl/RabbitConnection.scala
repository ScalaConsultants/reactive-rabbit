package io.scalac.amqp.impl

import com.rabbitmq.client.{Address, Channel, ConnectionFactory}

import io.scalac.amqp._

import org.reactivestreams.{Subscription, Subscriber}


private[amqp] class RabbitConnection(settings: ConnectionSettings) extends Connection {
  val factory = Conversions.toConnectionFactory(settings)
  val addresses: Array[Address] = settings.addresses.map(address ⇒
    new Address(address.host, address.port))(collection.breakOut)

  val underlying = factory.newConnection(addresses)

  def onChannel(f: Channel ⇒ Unit): Unit = {
    val channel = underlying.createChannel()
    f(channel)
    channel.close()
  }

  def declare(exchange: Exchange) =
    onChannel(_.exchangeDeclare(
      exchange.name,
      Conversions.toExchangeType(exchange.`type`),
      exchange.durable,
      exchange.autoDelete,
      exchange.internal,
      Conversions.toExchangeArguments(exchange)))

  override def declare(queue: Queue) =
    onChannel(_.queueDeclare(
      queue.name,
      queue.durable,
      queue.exclusive,
      queue.autoDelete,
      Conversions.toQueueArguments(queue)))

  override def deleteQueue(name: String) =
    onChannel(_.queueDelete(name))

  override def deleteExchange(name: String) =
    onChannel(_.exchangeDelete(name))

  override def consume(queue: String) =
    new QueuePublisher(underlying, queue)

  override def publish(exchange: String, routingKey: String) =
    new Subscriber[Message] {
      val delegate = new ExchangeSubscriber(
        channel = underlying.createChannel(),
        exchange = exchange)

      override def onError(t: Throwable) = delegate.onError(t)
      override def onSubscribe(s: Subscription) = delegate.onSubscribe(s)
      override def onComplete() = delegate.onComplete()

      override def onNext(message: Message) =
        delegate.onNext(Routed(
          routingKey = routingKey,
          message = message))
    }

  override def publish(exchange: String) =
    new ExchangeSubscriber(
      channel = underlying.createChannel(),
      exchange = exchange)

  override def publishDirectly(queue: String) =
    publish(exchange = "",
      routingKey = queue)
}