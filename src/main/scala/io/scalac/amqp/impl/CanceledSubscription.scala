package io.scalac.amqp.impl

import org.reactivestreams.Subscription


private[impl] object CanceledSubscription extends Subscription {
  override def request(n: Long) = ()
  override def cancel() = ()
}
