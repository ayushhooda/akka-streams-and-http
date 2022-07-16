package akkahttp.part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, MissingQueryParamRejection, Rejection, RejectionHandler}

object HandlingRejections extends App {

  implicit val system: ActorSystem = ActorSystem("HandlingRejections")

  val simpleRoute =
    path("api" / "myEndPoint") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val badRequestHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered $rejections")
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenRequestHandler: RejectionHandler = { (rejections: Seq[Rejection]) =>
    println(s"I have encountered $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandlers =
    handleRejections(badRequestHandler) {
      path("api" / "myEndPoint") {
        get {
          complete(StatusCodes.OK)
        }
      }
    }

  Http().newServerAt("localhost", 8080).bind(simpleRouteWithHandlers)

  // another way to handler rejections
  implicit val customRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case m: MethodRejection =>
        println(s"I got a method rejection: $m")
        complete("Rejected method!")
    }
    .handle {
      case m: MissingQueryParamRejection =>
        println(s"I got a query param rejection: $m")
        complete("Rejected query param!")
    }
    .result()

  Http().newServerAt("localhost", 8081).bind(simpleRoute)

}
