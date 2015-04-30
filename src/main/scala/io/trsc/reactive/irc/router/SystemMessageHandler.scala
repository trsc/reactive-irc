package io.trsc.reactive.irc.router

import akka.stream.stage.{SyncDirective, Context, StageState, StatefulStage}
import io.trsc.reactive.irc.protocol.IrcMessage

class SystemMessageHandler(channels: Seq[String]) extends StatefulStage[IrcMessage, String] {

  val joinCommands = channels.map(c => "JOIN " + c).iterator

  override def initial: StageState[IrcMessage, String] = new StageState[IrcMessage, String] {
    override def onPush(msg: IrcMessage, ctx: Context[String]): SyncDirective = msg match {
      case IrcMessage(_, "001", _) => emit(joinCommands, ctx)
      case IrcMessage(_, "PING", server) => emit(Iterator.single("PONG "), ctx)
      case _ => ctx.pull()
    }
  }
}
