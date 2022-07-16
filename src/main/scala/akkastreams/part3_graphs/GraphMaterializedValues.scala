package akkastreams.part3_graphs

import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {
  implicit val system: ActorSystem = ActorSystem("GraphMaterializedValues")

  val wordSource = Source(List[String]("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
    A composite component (sink)
    - prints out all strings which are lowercase
    - COUNTS the strings that are short (< 5 chars)
   */

  // step 1
  val complexWordSink: Sink[String, Future[Int]] = Sink.fromGraph(
    GraphDSL.createGraph(counter) { implicit builder => counterShape =>
      import GraphDSL.Implicits._

      // step 2 - SHAPES
      val broadcast = builder.add(Broadcast[String](2))
      val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(word => word.length < 5))

      // step 3 - connections
      broadcast ~> lowercaseFilter ~> printer
      broadcast ~> shortStringFilter ~> counterShape

      // step 4 - return shape
      SinkShape(broadcast.in)
    }
  )

  val shortStringCountFuture: Future[Int] = wordSource.toMat(complexWordSink)(Keep.right).run()
  import system.dispatcher
  shortStringCountFuture.onComplete {
    case Success(value) => println(s"Total number of short strings: $value")
    case Failure(exception) => println(s"Count of short string failed: $exception")
  }

  /**
   * Exercise
   */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.createGraph(counterSink) { implicit builder => counterSinkShape =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[B](2))
        val originalFlowShape = builder.add(flow)

        originalFlowShape ~> broadcast ~> counterSinkShape

        FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(identity)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()

  enhancedFlowCountFuture onComplete {
    case Success(value) => println(s"$value number of elements went through flow")
    case Failure(exception) => println(s"Something failed: $exception")
  }
}
