name := "scala-storm"

scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", "2.11.2")

organization := "com.github.velvia"

libraryDependencies += "org.apache.storm" % "storm-core" % "0.9.2-incubating" % "provided" exclude("junit", "junit")

scalacOptions ++= Seq("-feature", "-deprecation", "-Yresolve-term-conflict:package")

// When doing sbt run, fork a separate process.  This is apparently needed by storm.
fork := true
