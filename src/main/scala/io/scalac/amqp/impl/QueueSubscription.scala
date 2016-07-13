package io.scalac.amqp.impl

import com.google.common.primitives.Ints.saturatedCast
import com.rabbitmq.client._
import io.scalac.amqp.Delivery
import org.reactivestreams.{Subscriber, Subscription}

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm.{Ref, atomic}
import scala.util.control.NonFatal


private[amqp] class QueueSubscription(channel: Channel, queue: String, subscriber: Subscriber[_ >: Delivery])
  extends DefaultConsumer(channel) with Subscription {

  type Listener = () ⇒ Unit

  val demand = Ref(0L)

  /** Number of messages stored in this buffer is limited by channel QOS. */
  val buffer = Ref(Queue[Delivery]())
  val running = Ref(false)
  var closeRequested = Ref(false)
  val shutdownListeners = Ref(List[Listener]())

  override def finalize(): Unit =
    try closeChannel()
      finally super.finalize()

  override def handleCancel(consumerTag: String) = closeAndComplete()

  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException) = {
    if (sig.isInitiatedByApplication) {
      closeAndComplete()
    } else if (!channel.isInstanceOf[Recoverable]) {
      closeWithError(sig)
    }
  }

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]) = {
    val delivery = Conversions.toDelivery(envelope, properties, body)
    buffer.single.transform(_ :+ delivery)
    deliverRequested()
  }

  @tailrec
  private def deliverRequested(): Unit = {
    val go = atomic { implicit txn ⇒
      if (demand() > 0 && buffer().nonEmpty)
        running.transformAndExtract(r ⇒ (true, !r))
      else
        false
    }

    if (go) {
      //buffer and demand could only grow since last check
      atomic { implicit txn ⇒
        buffer().splitAt(saturatedCast(demand())) match {
          case (ready, left) ⇒
            buffer() = left
            demand -= ready.size
            ready
        }
      }.foreach(deliver)

      running.single.set(false)
      deliverRequested()
    }
  }

  def deliver(delivery: Delivery): Unit = try {
    if(channel.isOpen()) {
      channel.basicAck(delivery.deliveryTag.underlying, false)
      subscriber.onNext(delivery)
    }
  } catch {
    case NonFatal(exception) ⇒ closeWithError(exception)
  }

  def closeChannel(): Unit = synchronized {
    if (channel.isOpen()) {
      try channel.close() catch {
        case _: AlreadyClosedException ⇒ // ignore
      }
    }
  }

  override def request(n: Long) = n match {
    case n if n <= 0           ⇒
      closeWithError(new IllegalArgumentException("Rule 3.9: n <= 0"))
    case n if channel.isOpen() ⇒
      val newDemand = demand.single.transformAndGet(_ + n)
      newDemand match {
        case d if d < 0 ⇒ // 3.17: overflow
          closeWithError(new IllegalStateException("Rule 3.17: Pending + n > Long.MaxValue"))
        case d          ⇒
          Future(deliverRequested())
      }

    case _                   ⇒ // 3.6: nop
  }

  override def cancel() = close()

  def addShutdownListener(listener: Listener): Unit = {
    shutdownListeners.single.transform(listener :: _)
  }

  private def notifyShutdownListeners(): Unit = {
    shutdownListeners.single.get.foreach(_())
  }

  private def closeAndComplete(): Unit = close(subscriber.onComplete, subscriber.onError)

  private def closeWithError(cause: Throwable): Unit =
    close(() ⇒ subscriber.onError(cause), _ ⇒ subscriber.onError(cause))

  private val nop = () ⇒ ()

  private def close(
    onSuccess: () ⇒ Unit = nop,
    onError: Throwable ⇒ Unit = subscriber.onError
  ): Unit = {
    if (closeRequested.single.compareAndSet(false, true)) {
      try {
        closeChannel()
        onSuccess()
      } catch {
        case NonFatal(e) ⇒ onError(e)
      } finally {
        notifyShutdownListeners()
      }
    }
  }

  override def toString() = atomic { implicit txn ⇒
    s"QueueSubscription(channel=$channel, queue=$queue, subscriber=$subscriber, demand=${demand()}, " +
      s"buffer.size=${buffer().size})"
  }
}
