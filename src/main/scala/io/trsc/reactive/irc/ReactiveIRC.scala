package io.trsc.reactive.irc

import java.net.InetSocketAddress
import akka.actor.ActorSystem
import akka.stream.{BidiShape, ActorFlowMaterializer}
import akka.stream.scaladsl.{Flow, BidiFlow, Source, Tcp}
import akka.util.ByteString
import io.trsc.reactive.irc.protocol.IrcFrameDecoder

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

  val connection = Tcp().outgoingConnection(new InetSocketAddress("irc.freenode.net", 6666))

  val convertToByteString = (s: String) => ByteString(s)
  val convertToString = (b: ByteString) => b.utf8String

  val codec = BidiFlow() { b =>
    val outbound = b.add(Flow[String].map(_ + "\r\n").map(ByteString(_)))
    val inbound = b.add(Flow[ByteString].transform(() => new IrcFrameDecoder).map(_.utf8String))
    BidiShape(outbound, inbound)
  }

  val joiningSource = Source("PASS foobar" :: "NICK reactive-tester" :: "USER guest 0 * :Reactive Tester" :: "JOIN #akka" :: Nil)

  joiningSource.via(codec.join(connection)).runForeach { s =>
    println(s"received: $s")
  }

}
