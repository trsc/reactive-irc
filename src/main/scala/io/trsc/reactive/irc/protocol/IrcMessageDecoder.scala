package io.trsc.reactive.irc.protocol

import akka.stream.stage.{Context, PushStage, SyncDirective}
import akka.util.ByteString

class IrcMessageDecoder extends PushStage[ByteString, IrcMessage]{
  def onPush(bytes: ByteString, ctx: Context[IrcMessage]): SyncDirective = {
    var prefixEnd = 0
    val prefix = bytes.head match {
      case ':' =>
        prefixEnd = bytes.indexOf(' ')
        val res = Some(bytes.slice(1, prefixEnd).utf8String)
        prefixEnd += 1
        res
      case _ =>
        None
    }
    val commandEnd = bytes.indexOf(' ', from = prefixEnd)
    val command = bytes.slice(prefixEnd, commandEnd).utf8String

    var pos = commandEnd + 1
    var params: List[String] = Nil
    val len = bytes.length
    while (len > pos) {
      if (bytes(pos) == ':') {
        params = bytes.slice(pos + 1, len).utf8String :: params
        pos = len
      } else {
        var paramEnd = bytes.indexOf(' ', from = pos)
        if (paramEnd == -1) { paramEnd = len }
        params = bytes.slice(pos, paramEnd).utf8String :: params
        pos = paramEnd + 1
      }
    }
    ctx.push(IrcMessage(prefix, command, params.reverse))
  }
}
