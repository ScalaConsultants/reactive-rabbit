package io.scalac.amqp.impl

import java.util.concurrent.atomic.AtomicReference

import com.rabbitmq.client.Channel

import io.scalac.amqp.Message

import org.reactivestreams.{Subscription, Subscriber}


private[amqp] class ExchangeSubscriber(channel: Channel, exchange: String, routingKey: String)
  extends Subscriber[Message] {
  require(exchange.length <= 255, "exchange.length > 255")

  val subscription = new AtomicReference[Subscription]()

  override def onSubscribe(subscription: Subscription) = {
    this.subscription.set(subscription)
    subscription.request(1)
  }

  override def onNext(message: Message) =
    channel.basicPublish(
      exchange,
      routingKey,
      Conversions.toBasicProperties(message),
      message.body.toArray)
    subscription.get().request(1)

  override def onError(t: Throwable) =
    channel.close()

  override def onComplete() =
    channel.close()
}
