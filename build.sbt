name := """reactive-irc"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.10",
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC2",
  "com.typesafe.akka" %% "akka-http-scala-experimental" % "1.0-RC2",
  "org.scala-lang" % "scala-reflect" % "2.11.6",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.10" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
