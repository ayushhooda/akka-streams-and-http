package kafka_streams

import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.serialization._
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.time.Duration
import java.util.Properties

object WordCountApp extends App {

  import Serdes._

  val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka-broker1:9092")
    p
  }

  val builder: StreamsBuilder = new StreamsBuilder()

  val textLines: KStream[String, String] = builder.stream[String, String]("TextLinesTopic")

  val wordCounts: KTable[String, Long] = textLines
    .flatMapValues(textLines => textLines.toLowerCase.split(" "))
    .groupBy((_, word) => word)
    .count()(Materialized.as("counts-store"))

  wordCounts.toStream.to("WordsWithCountsTopic")

  val streams: KafkaStreams = new KafkaStreams(builder.build(), props)

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
