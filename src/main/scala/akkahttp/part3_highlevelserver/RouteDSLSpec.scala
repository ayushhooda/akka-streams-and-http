package akkahttp.part3_highlevelserver

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class Book(id:Int, author: String, title: String)

trait BookJsonProtocol extends DefaultJsonProtocol {
  implicit val bookFormat = jsonFormat3(Book)
}

class RouteDSLSpec extends AnyWordSpec with Matchers with BookJsonProtocol with ScalatestRouteTest {

  import RouteDSLSpec._

  "A digital library" should {
    "return all the books in the library" in {
      Get("/api/book") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK // assertion on status codes
        entityAs[List[Book]] shouldBe books // assertion on data
      }
    }

    "return a book by query parameter endpoint" in {
      Get("/api/book?id=2") ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Option[Book]] shouldBe books.find(_.id == 2)
      }
    }

    "return a book by calling the endpoint with the id in the path" in {
      Get("/api/book/2") ~> libraryRoute ~> check {
        // another way of fetching response
        response.status shouldBe StatusCodes.OK

        val strictEntityFut = response.entity.toStrict(1 second)
        val strictEntity = Await.result(strictEntityFut, 1 second)

        strictEntity.contentType shouldBe ContentTypes.`application/json`

        val book = strictEntity.data.utf8String.parseJson.convertTo[Option[Book]]
        book shouldBe books.find(_.id == 2)
      }
    }

    "insert a book into database" in {
      val newBook = Book(5, "a5", "t5")
      Post("/api/book", newBook) ~> libraryRoute ~> check {
        status shouldBe StatusCodes.OK
        assert(books.contains(newBook))
      }
    }

    "not accept other methods than POST and GET" in {
      Delete("/api/book") ~> libraryRoute ~> check {
        rejections should not be empty

        val methodRejections = rejections.collect {
          case r: MethodRejection => r
        }

        methodRejections.length shouldBe 2
      }
    }
  }

}

object RouteDSLSpec extends BookJsonProtocol with SprayJsonSupport {

  var books: List[Book] = List(
    Book(1, "a1", "t1"),
    Book(2, "a2", "t2"),
    Book(3, "a3", "t3"),
    Book(4, "a4", "t4")
  )

  /*
    GET /api/book - returns all the books in the library
    GET /api/book/x - return a single book with id x
    GET /api/book?id=x - same
    POST /api/book - adds a new book to the library
   */

  val libraryRoute: Route =
    pathPrefix("api" / "book") {
      get {
        (path(IntNumber) | parameter(Symbol("id").as[Int])) { id =>
          complete(books.find(_.id == id))
        } ~ pathEndOrSingleSlash {
          complete(books)
        }
      } ~ post {
        entity(as[Book]) { book =>
          books = books :+ book
          complete(StatusCodes.OK)
        } ~ complete(StatusCodes.BadRequest)
      }
    }
}