package io.scalac.amqp.impl

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

import akka.util.ByteString

import com.google.common.net.MediaType
import com.rabbitmq.client.{Envelope, AMQP}

import io.scalac.amqp._

import org.joda.time.DateTime


object Conversions {
  def toDeliveryMode(deliveryMode: Integer) =
    Option[Int](deliveryMode) match {
      case Some(2) => Persistent
      case _ => NonPersistent
    }

  def toDuration(value: String) = Option(value) match {
    case Some(ttl) => Duration(ttl.toLong, TimeUnit.MILLISECONDS)
    case _ => Duration.Inf
  }

  def toMessage(properties: AMQP.BasicProperties, body: Array[Byte]) =
    Message(
      body = ByteString(body),
      contentType = Option(properties.getContentType).map(MediaType.parse),
      contentEncoding = Option(properties.getContentEncoding),
      headers = properties.getHeaders.toMap.mapValues(_.toString),
      mode = toDeliveryMode(properties.getDeliveryMode),
      priority = Option(properties.getPriority),
      correlationId = Option(properties.getCorrelationId),
      replyTo = Option(properties.getReplyTo),
      expiration = toDuration(properties.getExpiration),
      messageId = Option(properties.getMessageId),
      timestamp = Option(properties.getTimestamp).map(new DateTime(_)),
      `type` = Option(properties.getType),
      userId = Option(properties.getUserId),
      appId = Option(properties.getAppId))

  def toDelivery(envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) =
    Delivery(
      message = toMessage(properties, body),
      deliveryTag = DeliveryTag(envelope.getDeliveryTag),
      exchange = envelope.getExchange,
      routingKey = envelope.getRoutingKey,
      redeliver = envelope.isRedeliver)
}
