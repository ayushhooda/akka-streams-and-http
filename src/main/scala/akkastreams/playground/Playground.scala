package akkastreams.playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object Playground extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("Playground")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  Source.single("hello, streams!").to(Sink.foreach(println)).run()

}
