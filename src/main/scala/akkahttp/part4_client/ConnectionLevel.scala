package akkahttp.part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Sink, Source}
import akkahttp.part4_client.PaymentSystemDomain.PaymentRequest

import scala.concurrent.Future
import scala.util.{Failure, Success}
import spray.json._

object ConnectionLevel extends App with PaymentJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("ConnectionLevel")
  import system.dispatcher

  val connectionFlow = Http().outgoingConnection("www.google.com")

  def oneOffRequest(request: HttpRequest): Future[HttpResponse] = {
    Source.single(request).via(connectionFlow).runWith(Sink.head)
  }

  oneOffRequest(HttpRequest()) onComplete {
    case Success(response) => println(s"Got successful response: $response")
    case Failure(exception) => println(s"Sending the request failed: $exception")
  }

  /*
    A small payments system
   */

  val creditCards = List(
    CreditCard("44", "123", "abc"),
    CreditCard("1234", "123", "abc"),
    CreditCard("34", "123", "abc"),
  )

  val paymentRequests = creditCards.map(PaymentRequest(_, "rrr", 99))

  val serverHttpRequests = paymentRequests.map { paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payment"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  }

  // recommended way of hitting server api via streams
  Source(serverHttpRequests)
    .via(Http().outgoingConnection("localhost", 8080))
    .to(Sink.foreach[HttpResponse](println))
    .run()


}
