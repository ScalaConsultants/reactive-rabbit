package io.scalac.amqp.impl

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

import akka.util.ByteString

import com.google.common.net.MediaType
import com.rabbitmq.client.{Envelope, AMQP}

import io.scalac.amqp._

import org.joda.time.DateTime


private object Conversions {
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
      body = ByteString(body),
      contentType = Option(properties.getContentType).map(MediaType.parse),
      contentEncoding = Option(properties.getContentEncoding),
      headers = Option(properties.getHeaders).map(_.toMap.mapValues(_.toString)).getOrElse(Map()),
      mode = toDeliveryMode(properties.getDeliveryMode),
      priority = Option(properties.getPriority).map(Integer2int),
      correlationId = Option(properties.getCorrelationId),
      replyTo = Option(properties.getReplyTo),
      expiration = toExpiration(properties.getExpiration),
      messageId = Option(properties.getMessageId),
      timestamp = Option(properties.getTimestamp).map(new DateTime(_)),
      `type` = Option(properties.getType),
      userId = Option(properties.getUserId),
      appId = Option(properties.getAppId))
  }

  def toDelivery(envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Delivery =
    Delivery(
      message = toMessage(properties, body),
      deliveryTag = DeliveryTag(envelope.getDeliveryTag),
      exchange = envelope.getExchange,
      routingKey = RoutingKey(envelope.getRoutingKey),
      redeliver = envelope.isRedeliver)

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
      .headers(message.headers)
      .deliveryMode(toDeliveryMode(message.mode))
      .priority(message.priority.map(int2Integer).orNull)
      .correlationId(message.correlationId.orNull)
      .replyTo(message.replyTo.orNull)
      .expiration(toExpiration(message.expiration))
      .messageId(message.messageId.orNull)
      .timestamp(message.timestamp.map(_.toDate).orNull)
      .`type`(message.`type`.orNull)
      .userId(message.userId.orNull)
      .appId(message.appId.orNull)
      .build()
  }
}
