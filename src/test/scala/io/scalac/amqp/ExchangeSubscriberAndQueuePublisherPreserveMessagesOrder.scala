package io.scalac.amqp

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ActorMaterializer
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.control.NonFatal

class ExchangeSubscriberAndQueuePublisherPreserveMessagesOrder extends FlatSpec with Matchers {

  val totalMessages = 10000
  val prefetch = 2
  val expected = new AtomicInteger(1)
  val finishedPromise = Promise[Unit]()
  val finished = finishedPromise.future

  "ExchangeSubscriber and QueuePublisher" should "preserve messages order" in {
    val connection = Connection()
    val consConn = Connection()
    val brokerReady = for {
      e <- connection.exchangeDeclare(Exchange("E", Direct, durable = false, autoDelete = true))
      q <- connection.queueDeclare(Queue("Q", autoDelete = true))
      b <- connection.queueBind("Q", "E", "q")
    } yield b
    Await.result(brokerReady, 10.seconds)

    //prefetch > 1 causes delivery to stream not ordered
    val qPublisher = consConn.consume(queue = "Q", prefetch = prefetch)
    val eSubscriber = connection.publish(exchange = "E", routingKey = "q")

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    Source.fromIterator(() => (1 to totalMessages).iterator)
      .map(i => Message(body = BigInt(i).toByteArray))
      .runWith(Sink.fromSubscriber(eSubscriber))

    Source.fromPublisher(qPublisher)
      .map(d => BigInt(d.message.body.toArray).toInt)
      .runWith(Sink.foreach(checkExpected))
    //Test takes 3s on decent PC so I give 10x.
    Await.result(finished, 30.seconds)
    Await.result(system.terminate(), 5.seconds)
    connection.shutdown()
    consConn.shutdown()
  }

  def checkExpected(actual: Int): Unit = {
    val exp = expected.getAndIncrement()
    try {
      actual should equal (exp +- (prefetch - 1))
      if (exp == totalMessages) {
        finishedPromise.success(())
      }
    } catch {
      case NonFatal(ex) =>
        finishedPromise.failure(ex)
    }
  }
  
}
