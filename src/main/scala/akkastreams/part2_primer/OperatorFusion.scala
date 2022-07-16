package akkastreams.part2_primer

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akkastreams.part1_recap.AkkaRecap.SimpleActor

object OperatorFusion extends App {

  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // this runs on same Actor i.e., single thread
  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()
  // this is operator/component fusion

  // This behaviour is equivalent to below classic actor implementation
  class MyActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        // flow operations
        val x2 = x + 1
        val x3 = x2 * 10
        // sink operation
        println(x3)
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor])
  (1 to 1000).foreach(simpleActor ! _)

  // complex flows
  val complexFlow = Flow[Int].map { x =>
    // simulating long computation
    Thread.sleep(1000)
    x + 1
  }

  val complexFlow2 = Flow[Int].map { x =>
    // simulating long computation
    Thread.sleep(1000)
    x * 10
  }

  // this takes 2 second before printing next element
  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()

  // async boundaries
  simpleSource.via(complexFlow).async // runs on one actor
    .via(complexFlow2).async // runs on another actor
    .to(simpleSink) // runs on third actor
    .run()

  // ordering guarantees
  // here ordering will be very strict, the element which comes first is fully processed first
  Source(1 to 3)
    .map(element => { println(s"Flow A: $element"); element })
    .map(element => { println(s"Flow B: $element"); element })
    .map(element => { println(s"Flow C: $element"); element })
    .runWith(Sink.ignore)

  // here ordering wrt to one element i.e., relative ordering is always guaranteed
  Source(1 to 3)
    .map(element => { println(s"Flow A: $element"); element }).async
    .map(element => { println(s"Flow B: $element"); element }).async
    .map(element => { println(s"Flow C: $element"); element }).async
    .runWith(Sink.ignore)
}
