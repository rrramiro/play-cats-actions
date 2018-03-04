name := "play-cats-actions"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.1",
  "org.typelevel" %% "cats-effect" % "0.9",
  "com.typesafe.play" %% "play" % "2.6.2" % "provided",
  "com.typesafe.play" %% "play-test" % "2.6.2" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
  "org.specs2" %% "specs2-core" % "3.9.4" % "test",
  "org.specs2" %% "specs2-scalacheck" % "3.9.4" % "test",
  "com.typesafe.play" %% "play-specs2" % "2.6.2" % "test",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

scalafmtTestOnCompile := true

scalafmtOnCompile := true
