package io.scalac.amqp.impl

import com.google.common.collect.ImmutableMap
import com.rabbitmq.client.{Address, ConnectionFactory}

import io.scalac.amqp.{Queue, Connection, ConnectionSettings}


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

  override def declare(queue: Queue) = {
    val channel = underlying.createChannel()

    val args = ImmutableMap.builder[String, Object]()

    // make copy of user arguments
    queue.arguments.foreach {
      case (key, value) => args.put(key, value)
    }

    // RabbitMQ extension: Per-Queue Message TTL
    if(queue.xMessageTtl.isFinite) {
      args.put("x-message-ttl", queue.xMessageTtl.toMillis.asInstanceOf[Object])
    }

    // RabbitMQ extension: Queue TTL
    if(queue.xExpires.isFinite) {
      args.put("x-expires", queue.xExpires.toMillis.asInstanceOf[Object])
    }

    // RabbitMQ extension: Queue Length Limit
    queue.xMaxLength.foreach(max => args.put("x-max-length", max.asInstanceOf[Object]))

    // RabbitMQ extension: Dead Letter Exchange
    queue.xDeadLetterExchange.foreach { exchange =>
      args.put("x-dead-letter-exchange", exchange.name)
      exchange.key.foreach(key =>
        args.put("x-dead-letter-routing-key", key.value))
    }

    channel.queueDeclare(queue.name, queue.durable, queue.exclusive, queue.autoDelete, args.build())
    channel.close()
  }

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