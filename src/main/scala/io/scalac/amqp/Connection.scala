package io.scalac.amqp

import com.typesafe.config.{Config, ConfigFactory}
import io.scalac.amqp.impl.RabbitConnection
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.Future


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
                   arguments: Map[String, String] = Map.empty): Future[Exchange.BindOk]

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
                arguments: Map[String, String] = Map.empty): Future[Queue.BindOk]

  /** Unbind a queue from an exchange.
    *
    * @param queue      the name of the queue
    * @param exchange   the name of the exchange
    * @param routingKey the routine key to use for the binding */
  def queueUnbind(queue: String, exchange: String, routingKey: String): Future[Queue.UnbindOk]

  /** Creates queue `Publisher`.
   *
   * Publisher keeps track of all his subscribers so they can only have one active subscription.
   * It also does necessary housekeeping related to the subscription life cycle. Every subscription
   * has its own channel and is isolated from others.
   * Returned instance is very lightweight and cheap to create.
   *
   * Keep in mind that some Reactive Streams implementations like Akka Streams do their own buffering.
   * Messages delivered to the buffer are considered delivered.
   *
   * @param queue Name of the consumed queue.
   * @param prefetch Number of unacknowledged messages in flight. It's beneficial to have this number higher
   *                 than 1 due to improved throughput. Setting this number to high may increase memory usage -
   *                 depending on average message size and speed of subscribers.
   * @param exclusive If set to true, the consumer will be exclusive and ony this consumer can access the queue. */
  def consume(queue: String, prefetch: Int = 20, exclusive: Boolean = false): Publisher[Delivery]

  /** Creates exchange `Subscriber` with fixed routing key.
    *
    * Each `Message` will be mapped to `Routed` with given routing key.
    *
    * @param exchange the name of the exchange
    * @param routingKey the routing key for messages published via this Subscriber
    */
  def publish(exchange: String, routingKey: String): Subscriber[Message]

  /** Creates `Subscriber` that publishes [[Routed]] messages to the exchange.
    *
    * @param exchange the name of the exchange
    */
  def publish(exchange: String): Subscriber[Routed]

  /** Creates an `Subscriber` that publishes its messages to the Default Exchange with @param queue routing key.
    *
    * See http://www.rabbitmq.com/tutorials/amqp-concepts.html
    *
    * @param queue the routing key for each message
    */
  def publishDirectly(queue: String): Subscriber[Message]

  /** Shutdowns underlying connection.
    * Publishers and subscribers are terminated and notified via `onError`.
    * This method waits for all close operations to complete. */
  def shutdown(): Future[Unit]
}
