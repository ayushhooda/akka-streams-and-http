package akkastreams.part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  /**
   * A composite source that concatenates 2 sources
   * - emits ALL the elements from the first source
   * - then ALL the elements from the second source
   */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat // here source is converted to shape type implicitly
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )

  //sourceGraph.to(Sink.foreach(println)).run()

  /*
    Complex Sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1 // here sink is converted to shape type implicitly
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  //firstSource.to(sinkGraph).run()

  /*
    Complex flow
   */
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      incrementerShape ~> multiplierShape // only shape type data structures has method ~>
      FlowShape(incrementerShape.in, multiplierShape.out)
    }
  )

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /**
   * Flow from a sink and a source
   */
  def fromSinkAndSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] = {
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)
        FlowShape(sinkShape.in, sourceShape.out)
      }
    )
  }

  // These type of method are available in library as well
  // Flow.fromSinkAndSource(_, _)
  // Flow.fromSinkAndSourceCoupled(_, _)
  // etc

}
