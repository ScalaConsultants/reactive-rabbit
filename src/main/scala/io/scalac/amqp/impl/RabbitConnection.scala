package io.scalac.amqp.impl

import java.io.IOException
import java.util

import com.rabbitmq.client.{Address, AlreadyClosedException, Channel}
import io.scalac.amqp._
import org.reactivestreams.{Subscriber, Subscription}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, blocking}

private[amqp] class RabbitConnection(settings: ConnectionSettings) extends Connection {
  val factory = Conversions.toConnectionFactory(settings)
  val isAutoRecovering = settings.automaticRecovery
  val addresses: Array[Address] = settings.addresses.map(address ⇒
    new Address(address.host, address.port))(collection.breakOut)

  val underlying = factory.newConnection(addresses)

  lazy val adminChannel = underlying.createChannel()

  def onChannel[T](f: Channel ⇒ T): T =
    if (isAutoRecovering) {
      f(adminChannel)
    } else {
      val channel = underlying.createChannel()
      try f(channel) finally {
        try (channel.close()) catch {
          case _: IOException | _: AlreadyClosedException ⇒ // don't care
        }
      }
    }

  def future[T](f: ⇒ T): Future[T] = Future(blocking(f))

  override def exchangeDeclare(exchange: Exchange) =
    future(onChannel(_.exchangeDeclare(
      exchange.name,
      Conversions.toExchangeType(exchange.`type`),
      exchange.durable,
      exchange.autoDelete,
      exchange.internal,
      Conversions.toExchangeArguments(exchange)))
    ).map(_ ⇒ Exchange.DeclareOk())

  override def exchangeDeclarePassive(exchange: String) =
    future(onChannel(_.exchangeDeclarePassive(exchange)))
      .map(_ ⇒ Exchange.DeclareOk())

  override def exchangeDelete(exchange: String, ifUnused: Boolean = false) =
    future(onChannel(_.exchangeDelete(exchange, ifUnused)))
      .map(_ ⇒ Exchange.DeleteOk())

  override def exchangeBind(destination: String, source: String, routingKey: String,
                            arguments: Map[String, String]) =
    future(onChannel(_.exchangeBind(destination, source, routingKey, arguments.asJava.asInstanceOf[util.Map[String, AnyRef]])))
      .map(_ ⇒ Exchange.BindOk())

  override def exchangeUnbind(destination: String, source: String, routingKey: String) =
    future(onChannel(_.exchangeUnbind(destination, source, routingKey)))
      .map(_ ⇒ Exchange.UnbindOk())

  override def queueDeclare(queue: Queue) =
    future(onChannel(_.queueDeclare(
      queue.name,
      queue.durable,
      queue.exclusive,
      queue.autoDelete,
      Conversions.toQueueArguments(queue)))
    ).map(ok ⇒ Queue.DeclareOk(
      queue = ok.getQueue,
      messageCount = ok.getMessageCount,
      consumerCount = ok.getConsumerCount
    ))

  override def queueDeclare() =
    future(onChannel(_.queueDeclare()))
      .map(ok ⇒ Queue(
        name = ok.getQueue,
        durable = false,
        exclusive = true,
        autoDelete = true
      ))

  override def queueDeclarePassive(queue: String) =
    future(onChannel(_.queueDeclarePassive(queue)))
      .map(ok ⇒ Queue.DeclareOk(
        queue = ok.getQueue,
        messageCount = ok.getMessageCount,
        consumerCount = ok.getConsumerCount
      ))

  override def queueDelete(queue: String, ifUnused: Boolean, ifEmpty: Boolean) =
    future(onChannel(_.queueDelete(queue, ifUnused, ifEmpty)))
      .map(ok ⇒ Queue.DeleteOk(ok.getMessageCount))

  override def queuePurge(queue: String) =
    future(onChannel(_.queuePurge(queue)))
      .map(ok ⇒ Queue.PurgeOk(ok.getMessageCount))

  override def queueBind(queue: String, exchange: String, routingKey: String,
                         arguments: Map[String, String]) =
    future(onChannel(_.queueBind(queue, exchange, routingKey, arguments.asJava.asInstanceOf[util.Map[String, AnyRef]])))
      .map(_ ⇒ Queue.BindOk())

  override def queueUnbind(queue: String, exchange: String, routingKey: String) =
    future(onChannel(_.queueUnbind(queue, exchange, routingKey)))
      .map(_ ⇒ Queue.UnbindOk())

  def declare(exchange: Exchange) =
    onChannel(_.exchangeDeclare(
      exchange.name,
      Conversions.toExchangeType(exchange.`type`),
      exchange.durable,
      exchange.autoDelete,
      exchange.internal,
      Conversions.toExchangeArguments(exchange)))

  override def consume(queue: String, prefetch: Int, exclusive: Boolean) =
    new QueuePublisher(underlying, queue, prefetch, exclusive)

  override def publish(exchange: String, routingKey: String) =
    new Subscriber[Message] {
      val channel = underlying.createChannel()
      val delegate = new ExchangeSubscriber(channel, exchange)

      override def onError(t: Throwable) = delegate.onError(t)
      override def onSubscribe(s: Subscription) = delegate.onSubscribe(s)
      override def onComplete() = delegate.onComplete()

      override def onNext(message: Message) =
        delegate.onNext(Routed(
          routingKey = routingKey,
          message = message))

      override def toString = s"ExchangeSubscriber(channel=$channel, exchange=$exchange, routingKey=$routingKey)"
    }

  override def publish(exchange: String) =
    new ExchangeSubscriber(
      channel = underlying.createChannel(),
      exchange = exchange)

  override def publishDirectly(queue: String) =
    publish(exchange = "",
      routingKey = queue)

  override def shutdown() = future(underlying.close())

  override def toString = s"RabbitConnection(settings=$settings)"
}