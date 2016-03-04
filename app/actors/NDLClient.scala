package actors

import akka.actor.Actor
import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient

import scalaz.\/.{left, right}

@Singleton
class NDLClient @Inject()(wsc: WSClient, config: Configuration) extends Actor {
  import NDLClient._
  import akka.pattern.pipe

  def receive = {
    case QueryString(queryString) =>
      wsc.url(ndlOpenSearchUrl(config)).withQueryString(queryString:_*).get().map { res =>
        res.status match {
          case 200 => right(res.body)
          case _ => left(s"Connect failed.\n${res.body}")
        }
      } pipeTo sender()
  }
}

object NDLClient {
  private[NDLClient] def ndlOpenSearchUrl(conf: Configuration) = conf.getString("settings.url.ndl.openSearch").get

  // Seqそのままで受け取ると型消去でチェックが働かないので入れ物のcase classを作る
  final case class QueryString(queryString: Seq[(String, String)])
}
