package io.trsc.reactive.irc

import java.net.InetSocketAddress

import akka.stream.scaladsl.StreamTcp
import com.typesafe.config.ConfigFactory

object ReactiveIRC extends App {

  val conf = ConfigFactory.load()

  val network = new InetSocketAddress(
    conf.getString("reactive-irc.network"),
    conf.getInt("reactive-irc.port")
  )

  val connection = StreamTcp().outgoingConnection(network)

}
