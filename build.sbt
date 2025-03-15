ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

ThisBuild / scalacOptions ++= Seq(
  "-source:future",
  "-encoding:utf-8",
  "-feature",
)

lazy val root = (project in file(".")).settings(
  name := "serdobot",
  idePackagePrefix.withRank(KeyRanks.Invisible) := Some(
    "io.github.ntdesmond.serdobot",
  ),
)

libraryDependencies ++= Seq(
  "dev.zio"      %% "zio"                 % "2.1.11",
  "dev.zio"      %% "zio-prelude"         % "1.0.0-RC31",
  "dev.zio"      %% "zio-config"          % "4.0.2",
  "dev.zio"      %% "zio-config-typesafe" % "4.0.2",
  "dev.zio"      %% "zio-config-magnolia" % "4.0.2",
  "dev.zio"      %% "zio-json"            % "0.7.4",
  "dev.zio"      %% "zio-test"            % "2.1.11" % Test,
  "dev.zio"      %% "zio-test-sbt"        % "2.1.11" % Test,
  "dev.zio"      %% "zio-test-magnolia"   % "2.1.11" % Test,
  "io.scalaland" %% "chimney"             % "1.5.0",
  "com.beachape" %% "enumeratum"          % "1.7.5",
  "dev.scalapy"  %% "scalapy-core"        % "0.5.3",
  "tf.tofu"      %% "tofu-zio2-logging"   % "0.13.6",
  "tf.tofu"      %% "tofu-logging-layout" % "0.13.6",
)

// ScalaPy config
fork := true

import ai.kien.python.Python

import java.nio.file.Files
import java.nio.file.Paths

lazy val python = Python(
  (
    Files.isDirectory(Paths.get(".sbt")),
    System.getProperty("os.name").toLowerCase(),
  ) match {
    case (false, _)                         => "python"
    case (true, os) if os.startsWith("win") => "./venv/Scripts/python"
    case (true, _)                          => "./venv/bin/python"
  },
)

lazy val javaOpts = python
  .scalapyProperties
  .get
  .map {
    case (k, v) => s"""-D$k=$v"""
  }
  .toSeq

javaOptions ++= javaOpts
