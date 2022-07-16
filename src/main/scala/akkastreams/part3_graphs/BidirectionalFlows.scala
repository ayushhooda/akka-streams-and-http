package akkastreams.part3_graphs

import akka.actor.ActorSystem
import akka.stream.BidiShape
import akka.stream.scaladsl.{Flow, GraphDSL}

object BidirectionalFlows extends App {

  implicit val system: ActorSystem = ActorSystem("BidirectionalFlows")

  // Example: Cryptography
  def encrypt(n: Int)(str: String): String = str.map(ch => (ch + n).toChar)
  def decrypt(n: Int)(str: String): String = str.map(ch => (ch - n).toChar)

  // bidiFlow
  val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>
    val encryptFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptFlowShape = builder.add(Flow[String].map(decrypt(3)))

    BidiShape(encryptFlowShape.in, encryptFlowShape.out, decryptFlowShape.in, decryptFlowShape.out)
    // BidiShape.fromFlows(encryptFlowShape, decryptFlowShape) // same thing as above
  }

}
