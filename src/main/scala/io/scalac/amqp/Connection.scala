package io.scalac.amqp

import scala.concurrent.Future

import java.io.IOException

import com.typesafe.config.{ConfigFactory, Config}

import io.scalac.amqp.impl.RabbitConnection

import org.reactivestreams.{Subscriber, Publisher}


object Connection {
  def apply(): Connection =
    apply(ConfigFactory.load())

  def apply(config: Config): Connection =
    apply(ConnectionSettings(config))

  def apply(settings: ConnectionSettings): Connection =
    new RabbitConnection(settings)
}


trait Connection {
  /** Declare an exchange.
    * This invocation does nothing if exchange with identical parameters already exists. */
  def exchangeDeclare(exchange: Exchange): Future[Exchange.DeclareOk]

  /** Declare an exchange passively; that is, check if the named exchange exists.
    *
    * @param exchange the name of the exchange */
  def exchangeDeclarePassive(exchange: String): Future[Exchange.DeclareOk]

  /** Delete an exchange.
    *
    * @param exchange the name of the exchange
    * @param ifUnused true to indicate that the exchange is only to be deleted if it is unused */
  def exchangeDelete(exchange: String, ifUnused: Boolean = false): Future[Exchange.DeleteOk]

  /** Bind an exchange to an exchange.
    *
    * @param destination the name of the exchange to which messages flow across the binding
    * @param source      the name of the exchange from which messages flow across the binding
    * @param routingKey  the routine key to use for the binding
    * @param arguments   other properties (binding parameters) */
  def exchangeBind(destination: String, source: String, routingKey: String,
                   arguments: Map[String, AnyRef] = Map.empty): Future[Exchange.BindOk]

  /** Unbind an exchange from an exchange.
    *
    * @param destination the name of the exchange to which messages flow across the binding
    * @param source      the name of the exchange from which messages flow across the binding
    * @param routingKey  the routine key to use for the binding */
  def exchangeUnbind(destination: String, source: String, routingKey: String): Future[Exchange.UnbindOk]

  /** Declare a queue.
    * This invocation does nothing if queue with identical parameters already exists. */
  def queueDeclare(queue: Queue): Future[Queue.DeclareOk]

  /** Actively declare a server-named, exclusive, auto-delete, non-durable queue. */
  def queueDeclare(): Future[Queue]

  /** Declare a queue passively; i.e., check if it exist.
    *
    * @param queue the name of the queue */
  def queueDeclarePassive(queue: String): Future[Queue.DeclareOk]

  /** Delete a queue.
    *
    * @param ifUnused true if the queue should be deleted only if not in use
    * @param ifEmpty  true if the queue should be deleted only if empty */
  def queueDelete(queue: String, ifUnused: Boolean = false, ifEmpty: Boolean = false): Future[Queue.DeleteOk]

  /** Purges the contents of the given queue.
    *
    * @param queue the name of the queue */
  def queuePurge(queue: String): Future[Queue.PurgeOk]

  /** Bind a queue to an exchange.
    *
    * @param queue      the name of the queue
    * @param exchange   the name of the exchange
    * @param routingKey the routine key to use for the binding
    * @param arguments  other properties (binding parameters) */
  def queueBind(queue: String, exchange: String, routingKey: String,
                arguments: Map[String, AnyRef] = Map.empty): Future[Queue.BindOk]

  /** Unbind a queue from an exchange.
    *
    * @param queue      the name of the queue
    * @param exchange   the name of the exchange
    * @param routingKey the routine key to use for the binding */
  def queueUnbind(queue: String, exchange: String, routingKey: String): Future[Queue.UnbindOk]

  def consume(queue: String): Publisher[Delivery]

  def publish(exchange: String, routingKey: String): Subscriber[Message]

  def publish(exchange: String): Subscriber[Routed]

  def publishDirectly(queue: String): Subscriber[Message]
}
