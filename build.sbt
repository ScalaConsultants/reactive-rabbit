// set the name of the project
name := "amqp"

version := "1.0"

organization := "io.scalac"

startYear := Some(2014)

// set the Scala version used for the project
scalaVersion := "2.11.2"

// append -deprecation to the options passed to the Scala compiler
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-target:jvm-1.7")

libraryDependencies ++= Seq(
  "org.reactivestreams" % "reactive-streams" % "0.4.0",
  "com.rabbitmq" % "amqp-client" % "3.3.5",
  "joda-time" % "joda-time" % "2.5",            // for DateTime, Duration
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",// for ByteString
  "com.google.guava" % "guava" % "18.0"         // for MediaType
)