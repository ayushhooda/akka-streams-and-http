package akkahttp.part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object LowLevelAPI extends App {

  implicit val system: ActorSystem = ActorSystem("LowLevelAPI")
  import system.dispatcher

  val serverSource = Http().newServerAt("localhost", 8000).connectionSource()

  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Accepted Incoming connection from: ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource.to(connectionSink).run()

  serverBindingFuture onComplete {
    case Success(_) => println("Server binding successful")
    case Failure(exception) => println(s"Server binding failed: $exception")
  }

  /*
    Method 1: Synchronously serve HTTP responses
   */
  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK, // Http 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP
            | </body>
            |</html>
            |""".stripMargin
        )
      )

    case req: HttpRequest =>
      req.discardEntityBytes()
      HttpResponse(
        status = StatusCodes.NotFound, // Http 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPs, the resource can't be found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

  // here source is API request, sink is http response
  //Http().newServerAt("localhost", 8080).connectionSource().runWith(httpSyncConnectionHandler)
  // short hand notation
   Http().newServerAt("localhost", 8080).bindSync(requestHandler)

  /*
    Method 2: Asynchronously serve HTTP responses
   */
  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future(HttpResponse(
        status = StatusCodes.OK, // Http 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP
            | </body>
            |</html>
            |""".stripMargin
        )
      ))

    case req: HttpRequest =>
      req.discardEntityBytes()
      Future(HttpResponse(
        status = StatusCodes.NotFound, // Http 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPs, the resource can't be found!
            | </body>
            |</html>
            |""".stripMargin
        )
      ))
  }

  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithAsyncHandler(asyncRequestHandler)
  }

//  Http().newServerAt("localhost", 8081).connectionSource().runWith(httpAsyncConnectionHandler)
  Http().newServerAt("localhost", 8081).bind(asyncRequestHandler) // short hand

  /*
    Method 3: stream based handler
   */
  val streamBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK, // Http 200
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP
            | </body>
            |</html>
            |""".stripMargin
        )
      )

      // path /search redirects to some other part of our website/webapp/microservice
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
      HttpResponse(
        StatusCodes.Found,
        headers = List(Location("http://www.google.com"))
      )

    case req: HttpRequest =>
      req.discardEntityBytes()
      HttpResponse(
        status = StatusCodes.NotFound, // Http 404
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPs, the resource can't be found!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  Http().newServerAt("localhost", 8082).bindFlow(streamBasedRequestHandler)
}
