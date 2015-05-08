package io.trsc.reactive.irc.example

import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow

object Routes extends Directives {

  def apply(handler: Flow[Message, Message, Any]) =
    get {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
      path("socket") {
        handleWebsocketMessages(handler)
      } ~
      getFromResourceDirectory("web")
    }

}
