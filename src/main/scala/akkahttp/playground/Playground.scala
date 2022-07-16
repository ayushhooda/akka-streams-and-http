package akkahttp.playground

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn

object Playground extends App {

  implicit val system: ActorSystem = ActorSystem("AkkaHttpPlayground")
  import system.dispatcher

  val simpleRoute = pathEndOrSingleSlash {
    complete(HttpEntity(
      ContentTypes.`text/html(UTF-8)`,
      """
        |<html>
        | <body>
        |   Hello
        | </body>
        |</html>
        |""".stripMargin
    ))
  }

  val bindingFuture = Http().newServerAt("localhost", 8080).bindFlow(simpleRoute)

  // wait for newline and then terminate the server
  StdIn.readLine()

  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

}
