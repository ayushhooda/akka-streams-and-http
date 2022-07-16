package akkastreams.part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackpressureBasics extends App {

  implicit val system = ActorSystem("BackpressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    // simulating long processing
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  //fastSource.to(slowSink).run() // fusing
  // not backpressure

 // fastSource.async.to(slowSink).run()
  // backpressure in place here

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x
  }

  fastSource.async
    .via(simpleFlow).async
    .to(slowSink)
  //  .run()

  /*
    reactions to backpressure (in order):
    - try to slow down if possible
    - buffer elements until there is more demand
    - drop down elements from the buffer if it overflows
    - tear down/kill the whole stream (failure)
   */
  val bufferedFlow = simpleFlow.buffer(10, OverflowStrategy.dropHead) // there are different OverflowStrategy
  fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
    .run()

  // throttling
  import scala.concurrent.duration._
  fastSource.throttle(2, 1.second).runWith(Sink.foreach(println))
  // here 2 elements will be printed every second, it is very useful tool

}
