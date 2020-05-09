// Copyright (C) 2020 Raymond M. Poling

name := "tags"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.11"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.26"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"
libraryDependencies += "com.dimafeng" %% "neotypes" % "0.13.2"
libraryDependencies += "org.neo4j.driver" % "neo4j-java-driver" % "1.7.5"

libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "4.8.3" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.26" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11" % "test"
)

scalacOptions in Test ++= Seq("-Yrangepos")

enablePlugins(JavaAppPackaging)

test in assembly := {}