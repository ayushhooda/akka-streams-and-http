package akkahttp.part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._

object DirectivesBreakdown extends App {

  implicit val system: ActorSystem = ActorSystem("DirectivesBreakdown")

  import system.dispatcher

  /**
   * Type #1: Filtering directives
   */
  val simpleHttpMethodRoute =
    post {
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute =
    path("about") {
      complete(
        HttpEntity(
          ContentTypes.`application/json`,
          """
            |<html>
            | <body>
            |  Hello from about page!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    }

  val complexPathRoute =
    path("api" / "myEndpoint") {
      complete(StatusCodes.OK)
    }

  val pathEndRoute =
    pathEndOrSingleSlash { // localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    }

  /**
   * Type #2: extraction directives
   */
  // GET /api/item/42
  val pathExtractionRoute =
  path("api" / "item" / IntNumber) { (itemNumber: Int) =>
    println(s"I've got a number in my path: $itemNumber")
    complete(StatusCodes.OK)
  }

  val pathMultiExtractionRoute =
    path("api" / "order" / IntNumber / IntNumber) { (id, inventory) =>
      println(s"I've got two numbers in my path: $id, $inventory")
      complete(StatusCodes.OK)
    }

  val queryParamExtractionRoute =
  // /api/item?id=45
    path("api" / "item") {
      parameter(Symbol("id").as[Int]) { id =>
        println(s"I've extracted the $id")
        complete(StatusCodes.OK)
      }
    }

  /**
   * Type #3: composite directives
   */
  val simpleNestedRoute = path("api" / "item") {
    get {
      complete(StatusCodes.OK)
    }
  }

  val compactSimpleNestedRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  val compactExtractRequestRoute =
    (path("controlEndPoint") & extractRequest & extractLog) { (request, log) =>
      log.info(s"I got the http request: $request")
      complete(StatusCodes.OK)
    }

  // /about and /aboutUs
  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~ path("aboutUs") {
      complete(StatusCodes.OK)
    }

  val dryRoute =
    (path("about") | path("aboutUs")) {
      complete(StatusCodes.OK)
    }

  // /42 and /?postId=42 and logic for both are same, it can be clubbed together
  val combinedIdRoute = {
    (path(IntNumber) | parameter(Symbol("postId").as[Int])) { (blogId: Int) =>
      // complex server logic
      complete(StatusCodes.OK)
    }
  }


  /**
   * Type #4: actionable directives
   */

  val completeOkRoute = complete(StatusCodes.OK)

  val failedRoute =
    path("notSupported") {
      failWith(new RuntimeException("Unsupported")) // completes with http 500
    }

  val routeWithRejection =
    path("home") {
      reject
    } ~ path("index") {
      completeOkRoute
    }


  Http().newServerAt("localhost", 8080).bind(queryParamExtractionRoute)

}
