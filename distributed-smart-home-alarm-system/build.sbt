val PekkoVersion = "1.6.0"

ThisBuild / organization := "pcd.alarm.system"
ThisBuild / version := "2.0.0"
ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    name := "clustered-smart-home-alarm-system",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-cluster-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
      "ch.qos.logback" % "logback-classic" % "1.6.3",
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
      "org.apache.pekko" %% "pekko-testkit" % PekkoVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.20" % Test
    )
  )

// Builds a single runnable fat jar so every cluster node can be started
// from the very same artefact: only the "role" and the NODE/SEED env vars differ.
assembly / assemblyOutputPath := baseDirectory.value / "target" / "app.jar"
assembly / mainClass := Some("pcd.alarm.system.SmartHomeAlarmSystem")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.discard
  case PathList("module-info.class")                              => MergeStrategy.discard

  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
