package controllers

import akka.actor._
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import entities.Book
import forms.SearchForm
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scalaz.\/.left
import scalaz.{-\/, \/, \/-}

@Singleton
class Application @Inject()(system: ActorSystem,
                            @Named("ndl-client-actor") ndlClient: ActorRef)
  extends Controller {
  import akka.pattern.ask
  import scala.concurrent.duration._
  import actors.Library

  def index = Action(Ok(views.html.index("Simple OPAC")))

  val bookForm = Form(
    mapping(
      "title" -> optional(text),
      "author" -> optional(text),
      "any" -> optional(text),
      "count" -> optional(number(min = 1, max = 200))
    )(SearchForm.apply)(SearchForm.unapply)
  )

  implicit val bookToJson = Json.writes[Book]
  implicit val timeout: akka.util.Timeout = 1 minute
  lazy val libraryActor = system.actorOf(Library.props)

  def books = Action.async { implicit req =>
    bookForm.bindFromRequest.fold(
      formWithError => {
        Future.successful(BadRequest("invalid request"))
      },
      {
        case validForm => (try {
          libraryActor.ask((validForm, ndlClient)).mapTo[\/[String, Seq[Book]]]
        } catch {
          case e: Exception => Future.successful(left(s"Server Error: ${e.toString}"))
        }).map {
          case \/-(books) => Ok(Json.toJson(books))
          case -\/(msg) => InternalServerError(msg)
        }
      }
    )
  }
}
