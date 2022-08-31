package akkastreams.part3_graphs

import akka.actor.ActorSystem
import akka.stream.{BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {

  implicit val system: ActorSystem = ActorSystem("BidirectionalFlows")

  // Example: Cryptography
  def encrypt(n: Int)(str: String): String = str.map(ch => (ch + n).toChar)
  def decrypt(n: Int)(str: String): String = str.map(ch => (ch - n).toChar)

  // bidiFlow
  val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>

    val encryptFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptFlowShape = builder.add(Flow[String].map(decrypt(3)))

    // BidiShape(encryptFlowShape.in, encryptFlowShape.out, decryptFlowShape.in, decryptFlowShape.out) // same thing as below
    BidiShape.fromFlows(encryptFlowShape, decryptFlowShape)
  }

  val unencryptedStrings = List("Akka", "is", "awesome", "testing", "bidirectional", "flows")
  val unencryptedSource = Source(unencryptedStrings)

  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)
      val bidi = builder.add(bidiCryptoStaticGraph)
      val encryptedSinkShape = builder.add(Sink.foreach[String](string => println(s"Encrypted: $string")))
      val unencryptedSinkShape = builder.add(Sink.foreach[String](string => println(s"Decrypted: $string")))

      unencryptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptedSinkShape
      unencryptedSinkShape <~ bidi.out2 ; bidi.in2 <~ encryptedSourceShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()

}
