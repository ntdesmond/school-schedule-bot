ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file(".")).settings(
  name := "serdobot",
  idePackagePrefix.withRank(KeyRanks.Invisible) := Some(
    "io.github.ntdesmond.serdobot",
  ),
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.1.9",
  "dev.zio" %% "zio-prelude" % "1.0.0-RC31",
  "dev.zio" %% "zio-config" % "4.0.2",
  "dev.zio" %% "zio-config-typesafe" % "4.0.2",
  "dev.zio" %% "zio-config-magnolia" % "4.0.2",
  "dev.zio" %% "zio-json" % "0.7.4",
  "dev.zio" %% "zio-test" % "2.1.9" % Test,
  "dev.zio" %% "zio-test-sbt" % "2.1.9" % Test,
  "dev.zio" %% "zio-test-magnolia" % "2.1.9" % Test,
  "io.scalaland" %% "chimney" % "1.5.0",
  "com.beachape" %% "enumeratum" % "1.7.5",
)
