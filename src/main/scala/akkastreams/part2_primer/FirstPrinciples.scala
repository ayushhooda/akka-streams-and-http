package akkastreams.part2_primer

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("FirstPrinciples")

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // sources
  val source: Source[Int, NotUsed] = Source(1 to 10)

  // sinks
  val sink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

  // connecting source with sink i.e., forming a graph
  val graph = source.to(sink)

  //graph.run()

  // flows transform elements
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x + 1)

  // combining source with flow
  val sourceWithFlow: Source[Int, NotUsed] = source.via(flow)

  // combining sink with flow
  val sinkWithFlow: Sink[Int, NotUsed] = flow.to(sink)

  // all there are same
  //sourceWithFlow.to(sink).run()
  //source.to(sinkWithFlow).run()
  //source.via(flow).to(sink).run() // most commonly used way

  // nulls are not allowed
  val illegalSource = Source.single[String](null)
  illegalSource.to(Sink.foreach(println)).run() // it will throw error
  // use Options instead

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source.single(List(1, 2, 3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(LazyList.from(1))
  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.future(Future(42))

  // various kinds of sinks
  val ignoreSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b) // do some of all elements in the stream

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5) // stops the stream after picking up first 5 elements
  // NOT have flatMap

  // source -> flow -> flow -> ... -> sink

  // syntactic sugars
  val mapSource = Source(1 to 10).map(x => x * 2) // Source(1 to 10).via(Flow[Int].map(x => x * 2))
  mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

}
