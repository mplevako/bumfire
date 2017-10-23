name := "bumfire"
version := "0.1"
description := "A poor-man's Campfire"
scalaVersion := "2.12.4"

scalacOptions in Compile ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-deprecation")

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.0.10",
    "com.github.scopt"  %% "scopt"     % "3.7.0",
    "org.scalatest"     %% "scalatest" % "3.0.3"   % Test
)