package io.trsc.reactive.irc

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import io.trsc.reactive.irc.router.{SystemMessageHandler, UnzipIrcMessages}
import io.trsc.reactive.irc.protocol.{IrcFrameStage, IrcMessage, IrcMessageStage, IrcNormalizeStage}

object ReactiveIRC {

  def listen(network: String, port: Int, nick: String, channels: Seq[String])(implicit s: ActorSystem): Source[IrcMessage, Unit] = {
    Source() { implicit builder =>
      import FlowGraph.Implicits._

      val connection = Tcp().outgoingConnection(new InetSocketAddress(network, port))
      val registeringSource = Source(s"NICK $nick" :: s"USER guest 0 * :${nick.capitalize}" :: Nil)
      // TODO proper logging
      val log = Flow[ByteString].map(s => {println(s"sending: ${s.utf8String}"); s})
      val logSys = Flow[IrcMessage].map(m => {println(s"received: $m"); m})

      val convertToByteString = Flow[String]
        .map(_ + "\r\n")
        .map(ByteString.apply)

      val decodeIrcMessages = Flow[ByteString]
        .transform(() => new IrcFrameStage)
        .transform(() => new IrcNormalizeStage)
        .transform(() => new IrcMessageStage)

      val systemMessageHandler = Flow[IrcMessage].transform(() => new SystemMessageHandler(channels))

      val merge = builder.add(MergePreferred[String](1))

      val splitMessages = builder.add(new UnzipIrcMessages)

      registeringSource ~> merge ~> convertToByteString ~> log ~> connection ~> decodeIrcMessages ~> splitMessages.in
      merge.preferred    <~    systemMessageHandler   <~  logSys  <~ splitMessages.systemMessages

      splitMessages.channelMessages
    }
  }

}