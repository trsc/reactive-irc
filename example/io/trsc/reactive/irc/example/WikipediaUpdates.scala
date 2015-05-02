package io.trsc.reactive.irc.example

import akka.actor.{Props, ActorSystem}
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}
import akka.stream.scaladsl.{Sink, Flow}
import akka.stream.stage.{Context, PushStage, SyncDirective}
import io.trsc.reactive.irc.ReactiveIRC
import io.trsc.reactive.irc.protocol.IrcMessage

import scala.util.Try

object WikipediaUpdates extends App {

  implicit val system = ActorSystem("wikipedia-update-listener")
  implicit val materializer = ActorFlowMaterializer()

  val ircSource = ReactiveIRC.listen("irc.wikimedia.org", 6667, "reactive-example", "#en.wikipedia" :: Nil)
  val extractMessage = Flow[IrcMessage].filter(msg => msg.command == "PRIVMSG" && msg.params.length > 0) map {
    case IrcMessage(_, _, params) => params.last
  }
  val countingSink = Sink.actorSubscriber(Props[CountingActor])

  ircSource.via(extractMessage).transform(() => new ChangedLinesExtractor).to(countingSink).run()

}

class ChangedLinesExtractor extends PushStage[String, Int] {

  private val changedLines = """.*\(([-\+]\d*)\).*""".r

  override def onPush(change: String, ctx: Context[Int]): SyncDirective = change match {
    case changedLines(s) => ctx.push(parseInt(s).getOrElse(0))
    case _ => ctx.push(0)
  }

  private def parseInt(s: String) = Try(Integer.parseInt(s)).toOption

}

class CountingActor extends ActorSubscriber {

  private var count = 0

  protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  def receive = {
    case OnNext(lines: Int) => count = count + lines; println(s"lines added to wikipedia since start: $count")
  }

}
