package io.scalac.amqp.impl

import java.util.concurrent.atomic.AtomicReference

import com.rabbitmq.client.Channel

import io.scalac.amqp.Routed

import org.reactivestreams.{Subscription, Subscriber}


private[amqp] class ExchangeSubscriber(channel: Channel, exchange: String)
  extends Subscriber[Routed] {
  require(exchange.length <= 255, "exchange.length > 255")

  val subscription = new AtomicReference[Subscription]()

  override def onSubscribe(subscription: Subscription) = {
    this.subscription.set(subscription)
    subscription.request(1)
  }

  override def onNext(routed: Routed) = {
    channel.basicPublish(
      exchange,
      routed.routingKey,
      Conversions.toBasicProperties(routed.message),
      routed.message.body.toArray)
    subscription.get().request(1)
  }

  override def onError(t: Throwable) =
    channel.close()

  override def onComplete() =
    channel.close()

  override def toString = s"ExchangeSubscriber(channel=$channel, exchange=$exchange)"
}
