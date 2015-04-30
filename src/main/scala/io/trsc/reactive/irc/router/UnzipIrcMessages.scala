package io.trsc.reactive.irc.router

import akka.stream.FanOutShape.{Name, Init}
import akka.stream.{OperationAttributes, FanOutShape}
import akka.stream.scaladsl.FlexiRoute
import io.trsc.reactive.irc.protocol.IrcMessage

class SplitMessageShape[A](init: Init[A] = Name[A]("SplitMessages")) extends FanOutShape[A](init) {
  val channelMessages = newOutlet[A]("channelMessages")
  val systemMessages = newOutlet[A]("systemMessages")
  protected override def construct(init: Init[A]) = new SplitMessageShape(init)
}

class UnzipIrcMessages extends FlexiRoute[IrcMessage, SplitMessageShape[IrcMessage]](
  new SplitMessageShape, OperationAttributes.name("SplitMessages")) {

  import FlexiRoute._

  override def createRouteLogic(p: PortT) = new RouteLogic[IrcMessage] {
    override def initialState =
      State(DemandFromAny(p.systemMessages, p.channelMessages)) {
        (ctx, _, message) =>
          message.command match {
            case "NOTICE" => ctx.emit(p.channelMessages)(message)
            case "PRIVMSG" => ctx.emit(p.channelMessages)(message)
            case _ => ctx.emit(p.systemMessages)(message)
          }
          SameState
      }
  }
}
