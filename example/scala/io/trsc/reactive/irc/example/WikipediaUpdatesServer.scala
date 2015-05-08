package io.trsc.reactive.irc.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorFlowMaterializer
import io.trsc.reactive.irc.ReactiveIRC
import io.trsc.reactive.irc.protocol.IrcMessage

import scala.util.{Failure, Success}

object WikipediaUpdatesServer extends App {

  implicit val system = ActorSystem("wikipedia-updates-system")
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  val broadcastSource = ReactiveIRC.listen("irc.wikimedia.org", 6667, "reactive-example", "#en.wikipedia" :: Nil) collect {
    case IrcMessage(_, _, params) => params.last
  } map { BroadcastMessage }

  val interface = "localhost"
  val port = args.headOption.map(_.toInt).getOrElse(7117)

  Http().bindAndHandle(Routes(ConnectionHandler(broadcastSource)), interface, port).onComplete {
    case Success(binding) ⇒
      val localAddress = binding.localAddress
      println(s"Started WikipediaUpdatesServer on ${localAddress.getHostName}:${localAddress.getPort}")
    case Failure(e) ⇒
      println(s"Failed to start WikipediaUpdatesServer with ${e.getMessage}")
      system.shutdown()
  }
}
