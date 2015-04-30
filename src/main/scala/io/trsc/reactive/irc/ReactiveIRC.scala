package io.trsc.reactive.irc

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.util.ByteString
import io.trsc.reactive.irc.flows.UnzipIrcMessages
import io.trsc.reactive.irc.protocol.{IrcFrameStage, IrcMessage, IrcMessageStage, IrcNormalizeStage}

object ReactiveIRC {

  def listen(network: String, port: Int, nick: String, /* unused*/ channels: Seq[String])(implicit s: ActorSystem): Source[IrcMessage, Unit] = {
    Source() { implicit builder =>
      import FlowGraph.Implicits._

      val connection = Tcp().outgoingConnection(new InetSocketAddress(network, port))
      val registeringSource = Source(s"NICK $nick" :: s"USER guest 0 * :${nick.capitalize}" :: Nil)
      // TODO proper logging
      val log = Flow[ByteString].map(s => {println(s"sending: ${s.utf8String}"); s})

      val convertToByteString = Flow[String]
        .map(_ + "\r\n")
        .map(ByteString.apply)

      val decodeIrcMessages = Flow[ByteString]
        .transform(() => new IrcFrameStage)
        .transform(() => new IrcNormalizeStage)
        .transform(() => new IrcMessageStage)

      // TODO properly implement the Request Response Protocol
      val testFlow = Flow[IrcMessage].filter(_.command == "001").map(_ => "JOIN #en.wikipedia\r\n").map(ByteString.apply)

      val merge = builder.add(MergePreferred[ByteString](1))

      val splitMessages = builder.add(new UnzipIrcMessages)

      registeringSource ~> convertToByteString ~> merge ~> log ~> connection ~> decodeIrcMessages ~> splitMessages.in
      merge.preferred    <~    testFlow    <~    splitMessages.systemMessages

      splitMessages.channelMessages
    }
  }

}