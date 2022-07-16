package akkastreams.part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}
import akka.util.Timeout

object AkkaRecap extends App {

  class SimpleActor extends Actor with ActorLogging with Stash {
    override def receive: Receive = {
      case "stashThis" =>
        stash()
      case "changeHandlerNow" =>
        unstashAll()
        context.become(anotherHandler)
      case "change" => context.become(anotherHandler)
      case msg => println(msg)
    }

    def anotherHandler: Receive = {
      case msg => println(s"another handler msg: $msg")
    }

    override def preStart(): Unit = {
      log.info("I am starting")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }

  // actor encapsulation
  val system = ActorSystem("AkkaRecap")
  // #1 you can only instantiate the actor through actor system
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")

  // #2 sending messages
  actor ! "hello"

  /*
    - messages are sent asynchronously
    - many actors (in millions) can share a few dozen threads
    - no need for locks, each message is processed/handled atomically
   */

  // changing actor behaviour + stashing
  actor ! "stashThis"
  actor ! "stashThis"
  actor ! "hello"
  actor ! "changeHandlerNow"

  // actors can spawn other actors i.e. can create child actors

  // guardians: /system, /user, / = root guardian

  // actors have a defined lifecycle: they can be started, stopped, suspended, resumed, restarted

  // stopping actors - context.stop

  actor ! PoisonPill

  // logging
  // supervision

  // configure Akka infrastructure: dispatchers, routers, mailboxes

  // schedulers
  import scala.concurrent.duration._
  import system.dispatcher
  system.scheduler.scheduleOnce(2.seconds) {
    actor ! "delayed happy birthday"
  }

  // akka patterns including FSM + ask pattern
  import akka.pattern.ask
  implicit val timeout: Timeout = Timeout(3.seconds)

  val future = actor ? "question"

  // pipe pattern
  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor], "anotherSimpleActor")
  future.mapTo[String].pipeTo(anotherActor) // this will send message to this actorRef when future completes
}
