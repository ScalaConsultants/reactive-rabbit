// set the name of the project
name := "amqp"

version := "1.0"

organization := "io.scalac"

startYear := Some(2014)

licenses := Seq("Apache License 2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-target:jvm-1.7")

libraryDependencies ++= Seq(
  "org.reactivestreams" % "reactive-streams" % "0.4.0",
  "com.rabbitmq" % "amqp-client" % "3.3.5",
  "joda-time" % "joda-time" % "2.5",              // for DateTime
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",  // for ByteString
  "com.google.guava" % "guava" % "18.0",          // for MediaType
  "com.google.code.findbugs" % "jsr305" % "3.0.0",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.google.inject" % "guice" % "3.0" % "test", // to make sbt happy
  "org.reactivestreams" % "reactive-streams-tck" % "0.4.0" % "test",
  "com.typesafe" % "config" % "1.2.1" % "test"    // Broker config
)