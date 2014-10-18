package io.scalac.amqp.impl

import com.rabbitmq.client.{Address, ConnectionFactory}
import io.scalac.amqp.{Connection, ConnectionSettings}

private[amqp] class RabbitConnection(settings: ConnectionSettings) extends Connection {
  val factory = new ConnectionFactory()
  factory.setVirtualHost(settings.virtualHost)
  factory.setUsername(settings.username)
  factory.setPassword(settings.password)

  settings.heartbeat match {
    case Some(interval) ⇒ factory.setRequestedHeartbeat(interval.toSeconds.toInt)
    case None           ⇒ factory.setRequestedHeartbeat(0)
  }

  settings.timeout match {
    case finite if finite.isFinite ⇒ factory.setConnectionTimeout(finite.toMillis.toInt)
    case _                         ⇒ factory.setConnectionTimeout(0)
  }

  factory.setNetworkRecoveryInterval(settings.recoveryInterval.toMillis.toInt)

  val addresses: Array[Address] = settings.addresses.map(address ⇒
    new Address(address.host, address.port))(collection.breakOut)

  val underlying = factory.newConnection(addresses)


  override def consume(queue: String) =
    new QueuePublisher(underlying, queue)

  override def publish(exchange: String, routingKey: String) =
    new ExchangeSubscriber(
      channel = underlying.createChannel(),
      exchange = exchange,
      routingKey = routingKey)

  override def publish(queue: String) =
    new ExchangeSubscriber(
      channel = underlying.createChannel(),
      exchange = "",
      routingKey = queue)
}