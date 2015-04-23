package io.trsc.reactive.irc

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{BidiFlow, Source, StreamTcp}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

object ReactiveIRC extends App {

  val conf = ConfigFactory.load()

  implicit val system = ActorSystem("reactive-irc")
  implicit val materializer = ActorFlowMaterializer()

  val network = new InetSocketAddress(
    conf.getString("reactive-irc.network"),
    conf.getInt("reactive-irc.port")
  )

  val connection = StreamTcp().outgoingConnection(network)

  val convertToByteString = (s: String) => ByteString(s)
  val convertToString = (b: ByteString) => b.utf8String

  val codec = BidiFlow(convertToByteString, convertToString).join(connection)

  Source(() => scala.io.Source.stdin.getLines()).map(_ + "\r\n").via(codec).runForeach { s =>
    println(s"response: $s")
  }

}
