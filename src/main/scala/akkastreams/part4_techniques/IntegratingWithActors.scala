package akkastreams.part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object IntegratingWithActors extends App {

  implicit val system: ActorSystem = ActorSystem("IntegratingWithActors")

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string $s")
        sender() ! s"$s$s" // double the string and return the reply
      case n: Int =>
        log.info(s"Just received a number $n")
        sender() ! (2 * n)
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "SimpleActor")

  val numberSource = Source(1 to 10)

  // actor as a flow
  implicit val timeout: Timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](4)(simpleActor)

  numberSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numberSource.ask[Int](4)(simpleActor).run() // same as above, without using the flow

}
