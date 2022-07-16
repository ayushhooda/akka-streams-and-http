package akkastreams.part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("MaterializingStreams")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import actorSystem.dispatcher

  // choosing materialised values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)
  // simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat) // same as below
  // simpleSource.viaMat(simpleFlow)(Keep.right)
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)

  graph.run() onComplete {
    case Success(_) => println("stream processing completed")
    case Failure(exception) => println(s"stream processing failed with: $exception")
  }

  // syntactic sugars
  //Source(1 to 10).runWith(Sink.reduce(_ + _)) // source.to(Sink.reduce)(Keep.right)
  Source(1 to 10).runReduce(_ + _) // same

  // backwards syntax
  Sink.foreach[Int](println).runWith(Source.single[Int](42))
  // both ways
  Flow[Int].map(x => 2 * x).runWith(simpleSource, simpleSink)

  // returning last element from source
  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last) // another way

  // word count in a stream of sentences
  val sentenceSource = Source(List(
    "Akka is awesome",
    "I love streams",
    "Materialized values are killing me"
  ))
  val wordCountSink = Sink.fold[Int, String](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g1 = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentenceSource.runWith(wordCountSink)
  val g3 = sentenceSource.runFold(0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)

}
