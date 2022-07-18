package akkahttp.part3_highlevelserver

import akka.actor.ActorSystem

object WebSocketsDemo extends App {

  implicit val system: ActorSystem = ActorSystem("WebSocketsDemo")
  import system.dispatcher



}
