package akkahttp.part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.scaladsl.{Sink, Source}
import akkahttp.part4_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import java.util.UUID
import scala.util.{Failure, Success}

object HostLevel extends App with PaymentJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("HostLevel")

  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

  // here order is not guaranteed
  Source(1 to 10)
    .map((HttpRequest(), _))
    .via(poolFlow)
    .map {
      case (Success(response), value) =>
        // VERY IMPORTANT
        response.discardEntityBytes()
        s"Request $value has received responses: $response"

      case (Failure(ex), value) =>
        s"Request $value has failed: $ex"
    }
    .runWith(Sink.foreach[String](println))


  // more example
  val creditCards = List(
    CreditCard("44", "123", "abc"),
    CreditCard("1234", "123", "abc"),
    CreditCard("34", "123", "abc"),
  )

  val paymentRequests = creditCards.map(PaymentRequest(_, "rrr", 99))

  val serverHttpRequests = paymentRequests.map { paymentRequest =>
    (HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payment"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    ), UUID.randomUUID().toString)
  }

  Source(serverHttpRequests)
    .via(Http().cachedHostConnectionPool[String]("localhost", 8080))
    .runForeach {
      case (Success(response), orderId) =>
        // VERY IMPORTANT
        response.discardEntityBytes()
        println(s"The orderId $orderId was successful and received response: $response")
        // you can do something with orderId now as it is returned back, this is quite useful

      case (Failure(ex), orderId) =>
        println(s"The orderId $orderId could not be completed: $ex")
    }

}
