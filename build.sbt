import sbtrelease.ReleasePlugin._

name := "scala-storm"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.9.2", "2.10.4")

organization := "com.github.velvia"

// sbt defaults to <project>/src/test/{scala,java} unless we put this in
unmanagedSourceDirectories in Test <<= Seq( baseDirectory( _ / "test" ) ).join

unmanagedSourceDirectories in Compile <<= Seq( baseDirectory( _ / "src" ) ).join

resolvers ++= Seq("clojars" at "http://clojars.org/repo/",
                  "clojure-releases" at "http://build.clojure.org/releases")

libraryDependencies += "storm" % "storm" % "0.8.2" % "provided" exclude("junit", "junit")

// This is to prevent error [java.lang.OutOfMemoryError: PermGen space]
javaOptions += "-XX:MaxPermSize=1g"

javaOptions += "-Xmx2g"

scalacOptions += "-Yresolve-term-conflict:package"

scalacOptions ++= {
	if (scalaVersion.value.startsWith("2.10")) {
		Seq("-feature", "-language:implicitConversions")
	} else {
		Seq()
	}
}

// When doing sbt run, fork a separate process.  This is apparently needed by storm.
fork := true

// set Ivy logging to be at the highest level - for debugging
ivyLoggingLevel := UpdateLogging.Full

// Aagin this may be useful for debugging
logLevel := Level.Info

//************ Publishing info *********
publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/velvia/ScalaStorm</url>
  <licenses>
    <license>
      <name>Apache</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:velvia/ScalaStorm.git</url>
    <connection>scm:git:git@github.com:velvia/ScalaStorm.git</connection>
  </scm>
  <developers>
    <developer>
      <id>velvia</id>
      <name>Evan Chan</name>
      <url>http://github.com/velvia</url>
    </developer>
  </developers>)

seq(releaseSettings: _*)
