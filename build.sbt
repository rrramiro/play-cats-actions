name := "play-cats-actions"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-effect" % "0.9",
  "com.typesafe.play" %% "play" % "2.6.2" % "provided",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % "test"
)

scalafmtTestOnCompile := true

scalafmtOnCompile := true
