package io.trsc.reactive.irc

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import io.trsc.reactive.irc.flows.UnzipIrcMessages
import io.trsc.reactive.irc.protocol.{IrcNormalizeStage, IrcFrameStage, IrcMessage, IrcMessageStage}

/**
 * Implementation
 *
 * Use FlexiRoute to split server messages and channel messages
 *
 * Server messages that need a reaction (e.g. user registered should trigger channel join, PING should trigger PONG)
 * will be designed with a cyclic graph
 *
 * the channel messages flow is the one that will be returned by the API
 *
 * the goal is something like this:
 *
 * ReactiveIRC.join("irc.freenode.net", 6666, "akka" :: "elixir-lang" :: Nil): Source[IrcMessage, Unit]
 *
 */
object ReactiveIRC extends App {

  implicit val system = ActorSystem("reactive-irc")
  implicit val materializer = ActorFlowMaterializer()

  // val registeringSource = Source("PASS foobar" :: "NICK reactive-tester" :: "USER guest 0 * :Reactive Tester" :: "JOIN #akka" :: Nil)

  val ircMessageSource = Source() { implicit builder =>
    import FlowGraph.Implicits._

    val connection = Tcp().outgoingConnection(new InetSocketAddress("irc.wikimedia.org", 6667))
    val registeringSource = Source("NICK reactive-tester" :: "USER guest 0 * :Reactive Tester" :: Nil)
    // TODO remove lame logging
    val log = Flow[ByteString].map(s => {println(s"sending: ${s.utf8String}"); s})

    val convertToByteString = Flow[String]
                                .map(_ + "\r\n")
                                .map(ByteString.apply)

    val decodeIrcMessages = Flow[ByteString]
                              .transform(() => new IrcFrameStage)
                              .transform(() => new IrcNormalizeStage)
                              .transform(() => new IrcMessageStage)

    // TODO properly implement the Request Response Protocol
    val testFlow = Flow[IrcMessage].filter(_.command == "376").map(_ => "JOIN #en.wikipedia\r\n").map(ByteString.apply)

    val merge = builder.add(MergePreferred[ByteString](1))

    val splitMessages = builder.add(new UnzipIrcMessages)

    val f = registeringSource ~> convertToByteString ~> merge ~> log ~> connection ~> decodeIrcMessages ~> splitMessages.in
                                                        merge.preferred    <~     testFlow    <~    splitMessages.systemMessages

    splitMessages.channelMessages
  }

  ircMessageSource.runForeach(println)

}