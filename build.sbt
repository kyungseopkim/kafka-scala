import Dependencies._

ThisBuild / scalaVersion     := "2.12.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "scala-client",
    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka" % "2.4.0",
//      "org.apache.hadoop" % "hadoop-aws" % "2.7.7",
//      "org.apache.hadoop" % "hadoop-common" % "2.7.7",
      "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.4",
      "net.liftweb" %% "lift-json" % "3.4.0",
      "log4j" % "log4j" % "1.2.17",
      "com.amazonaws" % "aws-java-sdk" % "1.11.717",
        scalaTest % Test
    )
  )

mainClass in assembly := Some("com.lucidmotors.data.message.kafka.KafkaS3Sinker")

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
  case PathList("javax", "activation", xs@_*) => MergeStrategy.last
  case PathList("com", "fasterxml", xs@_*) => MergeStrategy.last
  case PathList("org", "apache", xs@_*) => MergeStrategy.last
  case PathList("com", "google", xs@_*) => MergeStrategy.last
  case PathList("com", "codahale", xs@_*) => MergeStrategy.last
  case PathList("com", "yammer", xs@_*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard

  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case x => MergeStrategy.first
}

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
