import SonatypeKeys._

sonatypeSettings

name := "reactive-rabbit"

version := "1.1.0"

organization := "io.scalac"

startYear := Some(2014)

licenses := Seq("Apache License 2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

homepage := Some(url("https://github.com/ScalaConsultants/reactive-rabbit"))

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-target:jvm-1.8")

libraryDependencies ++= Seq(
  "org.reactivestreams"      %  "reactive-streams"         % "1.0.0",
  "com.rabbitmq"             %  "amqp-client"              % "3.6.1",
  "org.scala-stm"            %% "scala-stm"                % "0.7",
  "com.typesafe"             %  "config"                   % "1.3.0",            // Configuration
  "joda-time"                %  "joda-time"                % "2.9.2",            // for DateTime
  "org.joda"                 %  "joda-convert"             % "1.8.1",
  "com.google.guava"         %  "guava"                    % "19.0",             // for MediaType
  "com.google.code.findbugs" %  "jsr305"                   % "3.0.1",
  "org.scalatest"            %% "scalatest"                % "2.2.6"   % "test", // for TCK
  "org.reactivestreams"      %  "reactive-streams-tck"     % "1.0.0"   % "test",
  "com.typesafe.akka"        %% "akka-stream"              % "2.4.2"   % "test"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := {
  x ⇒ false
}

pomExtra := (
  <scm>
    <url>git@github.com:ScalaConsultants/reactive-rabbit.git</url>
    <connection>scm:git:git@github.com:ScalaConsultants/reactive-rabbit.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mkiedys</id>
      <name>Michał Kiędyś</name>
      <url>https://twitter.com/mkiedys</url>
    </developer>
  </developers>
)
