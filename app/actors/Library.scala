package actors

import akka.actor._
import akka.pattern.AskTimeoutException
import entities.Book
import forms.SearchForm
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.languageFeature.postfixOps
import scala.xml.XML
import scalaz.\/
import scalaz.\/.{left, right}
import shapeless._
import shapeless.ops.record.{Keys, Fields}
import shapeless.syntax.std.traversable._
import shapeless.tag
import shapeless.tag.@@

class Library extends Actor {
  import Library._
  import akka.pattern.{ask, pipe}
  import NDLClient.QueryString

  override def receive = {
    case RequestBooks(SearchForm(None, None, None, _), _) => sender ! BookData(right(Seq.empty[Book]))
    case RequestBooks(search, ndlClient) => (try {
      ndlClient.ask(QueryString(queryUrlBuilder(search))).mapTo[NDLResponse]
        .map { case NDLResponse(res) =>
          res.map { body =>
            for {
              item <- XML.loadString(body) \\ "item"
              book <- bookFields.map { attr => (item \ attr).headOption.fold("")(_.text) }.toHList[genBook.Repr].map(genBook.from)
            } yield book
          }
        }.map(BookData)
    } catch {
      case e: AskTimeoutException => Future.successful(left(s"Request Failed: \n$e"))
      case _ => Future.successful(left("Something wrong..."))
    }) pipeTo sender
  }
}

object Library {
  def props = Props[Library]
  implicit val timeout: akka.util.Timeout = 1 minute

  final case class RequestBooks(search: SearchForm, client: ActorRef)
  final case class BookData(books: String \/ Seq[Book])
  final case class NDLResponse(response: String \/ String)

  trait Cnt
  case class Search(title: Option[String],
                    creator: Option[String],
                    any: Option[String],
                    cnt: Option[Int @@ Cnt])

  implicit def toSearch(form: SearchForm) = {
    import form._
    Search(title, author, any, count.map(tag[Cnt](_)))
  }

  lazy val genBook = Generic[Book]
  lazy val lgenBook = LabelledGeneric[Book]

  lazy val bookFields = Keys[lgenBook.Repr].apply.toList.map(_.name)

  lazy val lgenSearch = LabelledGeneric[Search]

  object toQueryString extends Poly1 {
    implicit def caseString[T <: Symbol] = at[(T, Option[String])] {
      case (k, Some(v)) => Some(k.name -> v)
      case _ => None
    }
    implicit def caseCnt[T <: Symbol] = at[(T, Option[Int @@ Cnt])] {
      case (k, ov) => Some(k.name -> ov.getOrElse(20).toString)
    }
  }

  private[Library] def queryUrlBuilder(search: Search) =
    Fields[lgenSearch.Repr].apply(lgenSearch to search).map(toQueryString).toList.flatten
}
