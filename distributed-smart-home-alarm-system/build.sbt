ThisBuild / organization := "pcd.alarm.system"
ThisBuild / version := "1.0.0"
ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "smart-home-alarm-system",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % "1.6.0",
      "ch.qos.logback" % "logback-classic" % "1.6.1",
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.6.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.20" % Test
    )
  )