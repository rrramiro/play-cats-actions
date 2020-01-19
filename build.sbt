name := "play-cats-actions"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "com.typesafe.play" %% "play" % "2.8.0" % "provided",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
)

scalafmtTestOnCompile := true

scalafmtOnCompile := true
