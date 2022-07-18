package akkahttp.part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.scaladsl.Source
import akkahttp.part4_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import scala.util.{Failure, Success}

object RequestLevel extends App with PaymentJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("RequestLevel")
  import system.dispatcher

  val responseFut = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))

  responseFut onComplete {
    case Success(response) =>
      // VERY IMPORTANT
      response.discardEntityBytes()
      println(s"Request successful and returned: $response")
    case Failure(exception) =>
      println(s"Request failed: $exception")
  }

  val creditCards = List(
    CreditCard("44", "123", "abc"),
    CreditCard("1234", "123", "abc"),
    CreditCard("34", "123", "abc"),
  )

  val paymentRequests = creditCards.map(PaymentRequest(_, "rrr", 99))

  val serverHttpRequests = paymentRequests.map { paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("http://localhost:8080/api/payment"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  }

  Source(serverHttpRequests)
    .mapAsync(10)(request => Http().singleRequest(request)) // mapAsyncUnordered, if order is not required
    .runForeach(println)


}
