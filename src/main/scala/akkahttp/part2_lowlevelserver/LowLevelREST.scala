package akkahttp.part2_lowlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akkahttp.part2_lowlevelserver.GuitarDB.{CreateGuitar, FindAllGuitars, GuitarCreated}
import spray.json._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

case class Guitar(make: String, model: String)

object GuitarDB {
  case class CreateGuitar(guitar: Guitar)
  case class GuitarCreated(id: Int)
  case class FindGuitar(id: Int)
  case object FindAllGuitars
}
import GuitarDB._
class GuitarDB extends Actor with ActorLogging {

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0
  override def receive: Receive = {
    case FindAllGuitars =>
      log.info(s"Searching for all guitars")
      sender() ! guitars.values.toList

    case FindGuitar(id) =>
      log.info(s"Searching guitar by id: $id")
      sender() ! guitars.get(id)

    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar $guitar with id $currentGuitarId")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat2(Guitar)
}

object LowLevelREST extends App with GuitarStoreJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("LowLevelREST")
  import system.dispatcher

  /*
    GET on localhost:8080/api/guitar => All the guitars in the store
    POST on localhost:8080/api/guitar => insert the guitar into the store
    GET on localhost:8080/api/guitar?id=X => fetches the guitar associated with id X
   */

  // marshalling = object to JSON
  val simpleGuitar = Guitar("Fender", "Stratocaster")
  print(simpleGuitar.toJson.prettyPrint)

  // unmarshalling = JSON to object
  val simpleGuitarJSON =
    """
      |{
      |  "make": "Fender",
      |  "model": "Stratocaster"
      |}
      |""".stripMargin
  print(simpleGuitarJSON.parseJson.convertTo[Guitar])

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

  /*
  server code
   */
  implicit val defaultTimeout = Timeout(2 seconds)

  def getAllGuitars: Future[HttpResponse] = {
    val guitarsFuture: Future[List[Guitar]] = (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]
    guitarsFuture.map { guitars =>
      HttpResponse(
        entity = HttpEntity(
          ContentTypes.`application/json`,
          guitars.toJson.prettyPrint
        )
      )
    }
  }


  def getGuitarById(query: Uri.Query): Future[HttpResponse] = {
    query.get("id").map(_.toInt) match {
      case None => Future(HttpResponse(status = StatusCodes.NotFound))
      case Some(id) =>
        (guitarDB ? FindGuitar(id)).mapTo[Option[Guitar]].map {
          case None => HttpResponse(status = StatusCodes.NotFound)
          case Some(guitar) =>
            val guitarJSON = guitar.toJson.prettyPrint
            HttpResponse(status = StatusCodes.Found, entity = HttpEntity(ContentTypes.`application/json`, guitarJSON))
        }
    }
  }

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    // GET request handler
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
      val query = uri.query() // object like Map[String, String]
      if (query.isEmpty) {
        getAllGuitars
      } else {
        getGuitarById(query)
      }

    // POST request handler
    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>
        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]
        (guitarDB ? CreateGuitar(guitar)).mapTo[GuitarCreated].map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }

    // other request
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(status = StatusCodes.NotFound))
  }

  Http().newServerAt("localhost", 8080).bind(requestHandler)

}
