package io.trsc.reactive.irc.example

import akka.actor.{Terminated, ActorRef, Actor}
import akka.http.scaladsl.model.ws.TextMessage

sealed trait ConnectionEvent
case object ConnectionClosed extends ConnectionEvent
case class NewConnection(actorRef: ActorRef) extends ConnectionEvent
case class IncomingMessage(msg: String) extends ConnectionEvent
case class BroadcastMessage(msg: String) extends ConnectionEvent

class ConnectionSupervisor extends Actor {

  var subscribers = Set.empty[ActorRef]

  def receive = {
    case NewConnection(subscriber) =>
      context.watch(subscriber)
      subscribers += subscriber
      println("subscription added")
    case BroadcastMessage(msg) =>
      subscribers foreach { _ ! TextMessage.Strict(msg) }
      println("broadcasted: " + msg)
    case IncomingMessage(_) => println("TODO: we could have the client register for diff languages")
    case ConnectionClosed => println("TODO: proper logging")
    case Terminated(subscriber) =>
      subscribers -= subscriber
      println("subscription terminated")
  }

}
