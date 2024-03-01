ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.19"

lazy val root = (project in file("."))
  .settings(
    name := "scala-java17-flink"
  )

libraryDependencies += "org.apache.flink" %% "flink-scala" % "1.18.1"
libraryDependencies += "org.apache.flink" %% "flink-streaming-scala" % "1.18.1"
libraryDependencies += "org.apache.flink" % "flink-clients" % "1.18.1"

assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

