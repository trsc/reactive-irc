package io.trsc.reactive.irc.protocol

case class IrcMessage(prefix: Option[String], command: String, params: Seq[String] = Nil)
