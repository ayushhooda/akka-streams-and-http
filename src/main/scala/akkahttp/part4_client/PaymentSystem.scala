package akkahttp.part4_client

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class CreditCard(serialNumber: String, securityCode: String, account: String)

object PaymentSystemDomain {
  case class PaymentRequest(creditCard: CreditCard, receiverAccount: String, amount: Double)
  case object PaymentAccepted
  case object PaymentRejected
}

trait PaymentJsonProtocol extends DefaultJsonProtocol {
  implicit val creditCardJsonFormat = jsonFormat3(CreditCard)
  implicit val paymentJsonFormat = jsonFormat3(PaymentSystemDomain.PaymentRequest)
}

class PaymentValidator extends Actor with ActorLogging {
  import PaymentSystemDomain._
  override def receive: Receive = {
    case PaymentRequest(creditCard, receiverAccount, amount) =>
      log.info(s"${creditCard.account} is trying to send $amount to $receiverAccount")
      if (creditCard.serialNumber == "1234")
        sender() ! PaymentRejected
      else
        sender() ! PaymentAccepted
  }
}

object PaymentSystem extends App with PaymentJsonProtocol with SprayJsonSupport {

  implicit val system: ActorSystem = ActorSystem("PaymentSystem")
  import system.dispatcher
  import PaymentSystemDomain._
  implicit val timeout: Timeout = Timeout(2 second)

  val paymentValidator = system.actorOf(Props[PaymentValidator], "PaymentValidatorActor")

  val paymentRoute =
    path("api" / "payment") {
      post {
        entity(as[PaymentRequest]) { paymentRequest =>
          val validatorResponse = (paymentValidator ? paymentRequest).map {
            case PaymentRejected => StatusCodes.Forbidden
            case PaymentAccepted => StatusCodes.OK
            case _ => StatusCodes.BadRequest
          }
          complete(validatorResponse)
        }
      }
    }

  Http().newServerAt("localhost", 8080).bind(paymentRoute)

}
