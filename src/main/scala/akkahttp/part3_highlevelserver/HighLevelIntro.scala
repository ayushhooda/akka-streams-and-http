package akkahttp.part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

object HighLevelIntro extends App {

  implicit val system = ActorSystem("HighLevelIntro")

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") {
      complete(StatusCodes.OK)
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // chaining directives with ~
  val chainedRoute: Route =
    path("myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("home") {
      complete(StatusCodes.OK)
    }

  Http().newServerAt("localhost", 8080).bindFlow(simpleRoute)
  // Http().newServerAt("localhost", 8080).enableHttps(HttpsContext.httpsConnectionContext).bindFlow(simpleRoute) -> with https

}
