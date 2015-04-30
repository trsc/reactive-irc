package io.trsc.reactive.irc.example

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import io.trsc.reactive.irc.ReactiveIRC

object WikipediaUpdates extends App {

  implicit val system = ActorSystem("wikipedia-update-listener")
  implicit val materializer = ActorFlowMaterializer()

  ReactiveIRC.listen("irc.wikimedia.org", 6667, "reactive-example", "#en.wikipedia" :: Nil).runForeach(println)

}
