publishMavenStyle := true

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("SNAPSHOT")) Some(Opts.resolver.sonatypeSnapshots) else Some(Opts.resolver.sonatypeStaging)
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

releaseSettings
