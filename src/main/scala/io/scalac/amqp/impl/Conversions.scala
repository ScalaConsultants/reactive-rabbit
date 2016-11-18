package io.scalac.amqp.impl

import java.time.{ZoneId, ZonedDateTime}
import java.util
import java.util.Date
import java.util.concurrent.TimeUnit

import com.google.common.collect.ImmutableMap
import com.google.common.net.MediaType
import com.rabbitmq.client.{AMQP, ConnectionFactory, Envelope}
import io.scalac.amqp._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration


private object Conversions {
  def toConnectionFactory(settings: ConnectionSettings): ConnectionFactory = {
    val factory = new ConnectionFactory()
    factory.setVirtualHost(settings.virtualHost)
    factory.setUsername(settings.username)
    factory.setPassword(settings.password)
    factory.setAutomaticRecoveryEnabled(settings.automaticRecovery)

    // the initially requested heartbeat interval, in seconds; zero for none
    settings.heartbeat match {
      case Some(interval) ⇒ factory.setRequestedHeartbeat(interval.toSeconds.toInt)
      case None           ⇒ factory.setRequestedHeartbeat(0)
    }

    // connection establishment timeout in milliseconds; zero for infinite
    settings.timeout match {
      case finite if finite.isFinite ⇒ factory.setConnectionTimeout(finite.toMillis.toInt)
      case _                         ⇒ factory.setConnectionTimeout(0)
    }

    // how long will automatic recovery wait before attempting to reconnect
    factory.setNetworkRecoveryInterval(settings.recoveryInterval.toMillis)

    // enable SSL if needed
    (settings.sslProtocol, settings.sslContext) match {
      case (_, Some(context)) => factory.useSslProtocol(context)
      case (Some(protocol), _) => factory.useSslProtocol(protocol)
      case _ => ;
    }

    factory
  }

  /** Converts [[AMQP.BasicProperties]] and body to [[Message]] */
  def toMessage(properties: AMQP.BasicProperties, body: Array[Byte]): Message = {
    def toDeliveryMode(deliveryMode: Integer) = deliveryMode match {
      case mode: Integer if mode == 2 ⇒ Persistent
      case _                          ⇒ NonPersistent
    }

    def toExpiration(value: String) = Option(value) match {
      case Some(ttl) ⇒ Duration(ttl.toLong, TimeUnit.MILLISECONDS)
      case _         ⇒ Duration.Inf
    }

    Message(
      body            = body,
      contentType     = Option(properties.getContentType).map(MediaType.parse),
      contentEncoding = Option(properties.getContentEncoding),
      headers         = Option(properties.getHeaders).map(_.asScala.toMap.mapValues(_.toString)).getOrElse(Map()),
      mode            = toDeliveryMode(properties.getDeliveryMode),
      priority        = Option(properties.getPriority).map(Integer2int),
      correlationId   = Option(properties.getCorrelationId),
      replyTo         = Option(properties.getReplyTo),
      expiration      = toExpiration(properties.getExpiration),
      messageId       = Option(properties.getMessageId),
      timestamp       = Option(properties.getTimestamp).map((d) => ZonedDateTime.ofInstant(d.toInstant, ZoneId.systemDefault())),
      `type`          = Option(properties.getType),
      userId          = Option(properties.getUserId),
      appId           = Option(properties.getAppId))
  }

  def toDelivery(envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Delivery =
    Delivery(
      message     = toMessage(properties, body),
      deliveryTag = DeliveryTag(envelope.getDeliveryTag),
      exchange    = envelope.getExchange,
      routingKey  = envelope.getRoutingKey,
      redeliver   = envelope.isRedeliver)

  /** Converts [[Message]] to [[AMQP.BasicProperties]]. */
  def toBasicProperties(message: Message): AMQP.BasicProperties = {
    def toDeliveryMode(mode: DeliveryMode) = mode match {
      case NonPersistent ⇒ Integer.valueOf(1)
      case Persistent    ⇒ Integer.valueOf(2)
    }

    def toExpiration(value: Duration) = value match {
      case value if value.isFinite ⇒ value.toMillis.toString
      case _                       ⇒ null
    }

    new AMQP.BasicProperties.Builder()
      .contentType(message.contentType.map(_.toString).orNull)
      .contentEncoding(message.contentEncoding.orNull)
      .headers(message.headers.asJava.asInstanceOf[util.Map[String, AnyRef]])
      .deliveryMode(toDeliveryMode(message.mode))
      .priority(message.priority.map(int2Integer).orNull)
      .correlationId(message.correlationId.orNull)
      .replyTo(message.replyTo.orNull)
      .expiration(toExpiration(message.expiration))
      .messageId(message.messageId.orNull)
      .timestamp(message.timestamp.map((d) => Date.from(d.toInstant)).orNull)
      .`type`(message.`type`.orNull)
      .userId(message.userId.orNull)
      .appId(message.appId.orNull)
      .build()
  }

  /** Converts [[Queue]] attributes to map of AMQP attributes that can be used to declare queue.
    * This covers only RabbitMQ extensions. */
  def toQueueArguments(queue: Queue): ImmutableMap[String, Object] = {
    val builder = ImmutableMap.builder[String, Object]()

    // RabbitMQ extension: Per-Queue Message TTL
    if(queue.xMessageTtl.isFinite) {
      builder.put("x-message-ttl", queue.xMessageTtl.toMillis.asInstanceOf[Object])
    }

    // RabbitMQ extension: Queue TTL
    if(queue.xExpires.isFinite) {
      builder.put("x-expires", queue.xExpires.toMillis.asInstanceOf[Object])
    }

    // RabbitMQ extension: Queue Length Limit
    queue.xMaxLength.foreach(max ⇒ builder.put("x-max-length", max.asInstanceOf[Object]))

    // RabbitMQ extension: Queue Size Limit in Bytes
    queue.xMaxBytes.foreach(max ⇒ builder.put("x-max-length-bytes", max.asInstanceOf[Object]))

    // RabbitMQ extension: Dead Letter Exchange
    queue.xDeadLetterExchange.foreach { exchange ⇒
      builder.put("x-dead-letter-exchange", exchange.name)
      exchange.routingKey.foreach(builder.put("x-dead-letter-routing-key", _))
    }

    builder.build()
  }

  /** Converts [[Exchange]] attributes to map of AMQP attributes that can be used to declare exchange.
    * This covers only RabbitMQ extensions. */
  def toExchangeArguments(exchange: Exchange): ImmutableMap[String, Object] = {
    val builder = ImmutableMap.builder[String, Object]()

    // RabbitMQ extension: Alternate Exchange
    exchange.xAlternateExchange.foreach(builder.put("alternate-exchange", _))
    builder.build()
  }

  /** Maps case objects representing different exchange types to [[String]]. */
  def toExchangeType(`type`: Type): String = `type` match {
    case Direct  ⇒ "direct"
    case Topic   ⇒ "topic"
    case Fanout  ⇒ "fanout"
    case Headers ⇒ "headers"
  }
}
