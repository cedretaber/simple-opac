package actors

import java.net.URLEncoder

import akka.actor._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import shapeless._
import shapeless.syntax.std.traversable._

import scala.concurrent.duration._
import scala.languageFeature.postfixOps
import scala.xml.XML
import scalaz.\/
import scalaz.\/.right

import forms.SearchForm
import entities.Book

class Library extends Actor {
  import Library._
  import akka.pattern.{ask, pipe}
  import NDLClient.QueryString

  override def receive = {
    case (SearchForm(None, None, None, _), _) => right(Seq.empty[Book])
    case (search: SearchForm, ndlClient: ActorRef) => {
      ndlClient.ask(QueryString(queryUrlBuilder(search))).mapTo[\/[String, String]].map(_.map { body =>
        for {
          item <- XML.loadString(body) \\ "item"
          book <- bookFields.map { attr => (item \ attr).headOption.fold("")(_.text) }.toHList[genBook.Repr].map(genBook.from)
        } yield book
      }) pipeTo sender()
    }
  }
}

object Library {
  def props = Props[Library]
  implicit val timeout: akka.util.Timeout = 1 minute

  lazy val genBook = Generic[Book]

  case class Cnt(val n: Int) extends AnyVal { override def toString = n.toString }

  case class Search(title: Option[String],
                    creator: Option[String],
                    any: Option[String],
                    cnt: Option[Cnt])

  implicit def toSearch(form: SearchForm) = {
    import form._
    Search(title, author, any, count.map(Cnt))
  }

  private[this] def getFieldsStrings(klass: Class[_]) = klass.getDeclaredFields.map(_.getName)

  type FourString = String::String::String::String::HNil
  val searchFields = getFieldsStrings(classOf[Search]).toHList[FourString].get
  val bookFields = getFieldsStrings(classOf[Book])

  object toQueryString extends Poly2 {
    implicit val caseString = at[String, Option[String]] {
      case (k, Some(v)) => Some(k -> v)
      case _ => None
    }
    implicit val caseInt = at[String, Option[Cnt]] { (k, ov) => Some(k -> ov.getOrElse(20).toString) }
  }

  private[Library] def queryUrlBuilder(search: Search) =
    searchFields.zipWith(Generic[Search].to(search))(toQueryString).toList.flatten
}
