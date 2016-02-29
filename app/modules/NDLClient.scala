package modules

import actors.{NDLClient => NDLClientActor}
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class NDLClient extends AbstractModule with AkkaGuiceSupport {
  def configure = bindActor[NDLClientActor]("ndl-client-actor")
}
