package akkastreams.part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Source}

object GraphCycles extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GraphCycles")

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementalShape = builder.add(Flow[Int].map{ x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementalShape
                   mergeShape <~ incrementalShape // this forms cycle, here value of source will again and again be incremented in cycles

    ClosedShape
  }

  //RunnableGraph.fromGraph(accelerator).run()
  // graph cycle deadlock

  /**
   * Solution 1: MergePreferred instead of Merge
   */
  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementalShape = builder.add(Flow[Int].map{ x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementalShape
    mergeShape.preferred <~ incrementalShape // this forms cycle, here value of source will again and again be incremented in cycles

    ClosedShape
  }

  //RunnableGraph.fromGraph(actualAccelerator).run()

  /**
   * Solution 2: buffers
   */
  val bufferedAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map{ x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape // this forms cycle, here value of source will again and again be incremented in cycles

    ClosedShape
  }

  RunnableGraph.fromGraph(bufferedAccelerator).run()

  /**
   * Cycles risk deadlocking
   * - we have to add bounds to the number of elements in the cycle
   * - tradeoff is boundedness vs liveness
   */


}
