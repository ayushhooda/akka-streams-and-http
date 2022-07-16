package akkahttp.part3_highlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import spray.json._

case class Player(nickName: String, characterClass: String, level: Int)

object GameAreaMap {
  case object GetAllPlayers
  case class GetPlayer(nickName: String)
  case class GetPlayersByClass(characterClass: String)
  case class AddPlayer(player: Player)
  case class RemovePlayer(player: Player)
  case object OperationSuccess
}

class GameAreaMap extends Actor with ActorLogging {
  import GameAreaMap._

  var players: Map[String, Player] = Map[String, Player]()

  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all players")
      sender() ! players.values.toList

    case GetPlayer(nickName) =>
      log.info(s"Getting player with nickname: $nickName")
      sender() ! players.get(nickName)

    case GetPlayersByClass(characterClass) =>
      log.info(s"Getting players with class: $characterClass")
      sender() ! players.values.toList.filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Adding player")
      players = players + (player.nickName -> player)
      sender() ! OperationSuccess

    case RemovePlayer(player) =>
      log.info(s"Removing player")
      players = players - player.nickName
      sender() ! OperationSuccess
  }
}

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerJsonFormat = jsonFormat3(Player)
}


object MarshallingJSON extends App with PlayerJsonProtocol with SprayJsonSupport {

  implicit val system: ActorSystem = ActorSystem("MarshallingJSON")
  import system.dispatcher
  import GameAreaMap._

  implicit val timeout: Timeout = Timeout(5 seconds)

  val gameAreaMap = system.actorOf(Props[GameAreaMap], "gameAreaMap")

  val players = List(
    Player("a1", "c1", 10),
    Player("a2", "c2", 5),
    Player("a3", "c3", 2)
  )

  players.foreach(player => gameAreaMap ! AddPlayer(player))

  /*
    - GET /api/player, returns all the players in the map, as JSON
    - GET /api/player/nickname, returns the player with the given nickname, as JSON
    - GET /api/player?nickname=X, does the same
    - GET /api/player/class/charClass, returns all the players with the given character class
    - POST /api/player with JSON payload, adds the player to the map
    - DELETE /api/player with JSON payload, removed the player from the map
   */

  val route = pathPrefix("api" / "player") {
    get {
      path("class" / Segment) { characterClass =>
        val playerListFut = (gameAreaMap ? GetPlayersByClass(characterClass)).mapTo[List[Player]]
        complete(playerListFut) // This done automatically by SprayJsonSupport trait
      } ~
      (path(Segment) | parameter(Symbol("nickname").as[String])) { nickname =>
        complete((gameAreaMap ? GetPlayer(nickname)).mapTo[Option[Player]])
      } ~
      pathEndOrSingleSlash {
        complete((gameAreaMap ? GetAllPlayers).mapTo[List[Player]])
      }
    } ~
    post {
      entity(as[Player]) { player =>
        complete((gameAreaMap ? AddPlayer(player)).map(_ => StatusCodes.OK))
      }
    } ~
    delete {
      entity(as[Player]) { player =>
        complete((gameAreaMap ? RemovePlayer(player)).map(_ => StatusCodes.OK))
      }
    }
  }

  Http().newServerAt("localhost", 8080).bind(route)

}
