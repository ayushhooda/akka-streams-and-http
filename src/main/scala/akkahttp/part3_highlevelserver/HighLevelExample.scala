package akkahttp.part3_highlevelserver

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akkahttp.part2_lowlevelserver.{Guitar, GuitarDB, GuitarStoreJsonProtocol}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

object HighLevelExample extends App with GuitarStoreJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("HighLevelExample")
  import system.dispatcher

  import GuitarDB._
  /*
    GET on localhost:8080/api/guitar => All the guitars in the store
    GET on localhost:8080/api/guitar?id=X => fetches the guitar associated with id X
   */

  /*
  setup
   */
  val guitarDB = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
  val guitarList = List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Abc"),
    Guitar("Martin", "Def")
  )

  guitarList.foreach { guitar =>
    guitarDB ! CreateGuitar(guitar)
  }

  implicit val timeout: Timeout = Timeout(2 seconds)

  private def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val guitarServerRoute =
    path("api" / "guitar") {
      get {
        val guitarsFut = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]].map(_.toJson.prettyPrint).map(toHttpEntity)
        complete(guitarsFut)
      } ~ parameter(Symbol("id").as[Int]) { (guitarId: Int) =>
        get {
          val guitarsFut = (guitarDB ? FindGuitar(guitarId)).mapTo[Option[Guitar]].map(_.toJson.prettyPrint).map(toHttpEntity)
          complete(guitarsFut)
        }
      }
    }

  Http().newServerAt("localhost", 8080).bind(guitarServerRoute)

}
