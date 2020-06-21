
import sbt._

object Dependencies {

  object Versions {
    val cats = "2.1.1"
    val fs2 = "2.4.2"
    val catsEffect = "2.1.3"
    val log4cats = "1.0.1"
    val scodec = "1.11.7"
  }

  val mtproto  = Seq(
    "org.rudogma" %% "supertagged" % "2.0-RC1",

    "ch.qos.logback"       % "logback-classic"          % "1.2.3",
    "net.logstash.logback" % "logstash-logback-encoder" % "5.1",
    "org.slf4j" % "slf4j-api" % "1.7.29",
    "io.chrisdavenport" %% "log4cats-core" % Versions.log4cats,
    "io.chrisdavenport" %% "log4cats-slf4j" % Versions.log4cats,

    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.catsEffect,

    "co.fs2" %% "fs2-core" % Versions.fs2,
    "co.fs2" %% "fs2-io" % Versions.fs2,

    "org.scodec" %% "scodec-core" % Versions.scodec,
    "org.scodec" %% "scodec-stream" % "2.0.0",

    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.scalamock" %% "scalamock" % "4.3.0" % Test,
  )


}
