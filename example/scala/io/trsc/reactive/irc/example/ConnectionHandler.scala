package io.trsc.reactive.irc.example

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._

object ConnectionHandler {
  def apply(broadcastSource: Source[BroadcastMessage, _])(implicit sys: ActorSystem) = new ConnectionHandler(broadcastSource).flow
}

class ConnectionHandler(broadcastSource: Source[BroadcastMessage, _])(implicit sys: ActorSystem) extends Directives {

  private val connectionSupervisor = sys.actorOf(Props[ConnectionSupervisor])

  def flow = Flow(wrappedSupervisorSink, clientOutSource)(Keep.right) { implicit builder =>
    (supervisor, out) =>
      import akka.stream.scaladsl.FlowGraph.Implicits._

      val broadcast = builder.add(broadcastSource)
      val collectTextMessage = builder.add(collectTextMessageFlow)
      val convertToIncomingMessage = builder.add(convertToIncomingMessageFlow)
      val merge = builder.add(Merge[ConnectionEvent](3))

      broadcast                                            ~> merge.in(0)
      collectTextMessage ~> convertToIncomingMessage       ~> merge.in(1)
      builder.matValue   ~> convertActorRefToNewConnection ~> merge.in(2); merge ~> supervisor

      (collectTextMessage.inlet, out.outlet)
  }

  private val wrappedSupervisorSink = Sink.actorRef[ConnectionEvent](connectionSupervisor, ConnectionClosed)
  private val clientOutSource = Source.actorRef[Message](1, OverflowStrategy.fail)

  private val collectTextMessageFlow =  Flow[Message] collect { case TextMessage.Strict(msg) => msg }
  private val convertToIncomingMessageFlow = Flow[String] map { IncomingMessage }

  private val convertActorRefToNewConnection = Flow[ActorRef] map { NewConnection }

}
