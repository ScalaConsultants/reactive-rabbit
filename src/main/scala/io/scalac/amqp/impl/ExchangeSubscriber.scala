package io.scalac.amqp.impl

import java.util.Objects.requireNonNull
import java.util.concurrent.atomic.AtomicReference

import com.rabbitmq.client.Channel
import io.scalac.amqp.Routed
import org.reactivestreams.{Subscriber, Subscription}


private[amqp] class ExchangeSubscriber(channel: Channel, exchange: String)
  extends Subscriber[Routed] {
  require(exchange.length <= 255, "exchange.length > 255")

  val active = new AtomicReference[Subscription]()

  override def onSubscribe(subscription: Subscription) =
    active.compareAndSet(null, subscription) match {
      case true  ⇒ subscription.request(1)
      case false ⇒ subscription.cancel() // 2.5: cancel
    }

  override def onNext(routed: Routed) = {
    channel.basicPublish(
      exchange,
      routed.routingKey,
      Conversions.toBasicProperties(routed.message),
      routed.message.body.toArray)
    active.get().request(1)
  }

  /** Our life cycle is bounded to underlying `Channel`. */
  override def onError(t: Throwable) = {
    requireNonNull(t)
    channel.close()
  }

  /** Our life cycle is bounded to underlying `Channel`. */
  override def onComplete() = channel.close()

  override def toString = s"ExchangeSubscriber(channel=$channel, exchange=$exchange)"
}
