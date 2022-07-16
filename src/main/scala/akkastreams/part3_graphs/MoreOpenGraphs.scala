package akkastreams.part3_graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ClosedShape, FanOutShape2, UniformFanInShape}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system: ActorSystem = ActorSystem("MoreOpenGraphs")
  /*
    Example: Max3 operator
    - 3 inputs of type int
    - the maximum of 3
   */

  // step 1
  val maxStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // step 2
    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    // step 3
    max1.out ~> max2.in0

    // step 4
    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is: $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val max3Shape = builder.add(maxStaticGraph)
      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink
      ClosedShape
    }
  )

  //max3RunnableGraph.run()

  /*
    Non uniform fanout shape
    Processing bank transactions
    Txn suspicious if amount > 10000
    Streams component for transactions
   */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("123", "person1", "person2", 100, new Date),
    Transaction("123", "person1", "person2", 100000, new Date),
    Transaction("123", "person1", "person2", 100, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txn => println(s"Suspicious transaction ID: $txn"))

  // step 1
  val suspiciousTransactionStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // step 2 - define SHAPES
    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

    // step 3 - tie SHAPES
    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    // step 4 - return SHAPE
    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  // step 1
  val suspiciousTransactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - define SHAPES
      val suspiciousTxnShape = builder.add(suspiciousTransactionStaticGraph)

      // step 3 - tie SHAPES
      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      // step 4 - return shape
      ClosedShape
    }
  )

  suspiciousTransactionRunnableGraph.run()
}
