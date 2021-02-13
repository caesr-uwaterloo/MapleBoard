// See README.md for license details.

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // If we're building with Scala > 2.11, enable the compile option
    //  switch to support our anonymous Bundle definitions:
    //  https://github.com/scala/bug/issues/10047
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
      case _ => Seq("-Xsource:2.11")
    }
  }
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // Scala 2.12 requires Java 8. We continue to generate
    //  Java 7 compatible code for Scala 2.11
    //  for compatibility with old clients.
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
        Seq("-source", "1.7", "-target", "1.7")
      case _ =>
        Seq("-source", "1.8", "-target", "1.8")
    }
  }
}

name := "coherence"

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel3" -> "3.3-SNAPSHOT",
  "chisel-iotesters" -> "[1.2.5,1.3-SNAPSHOT[",
  )




lazy val commonSettings = Seq (
 resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
 ),
  organization := "uwaterloo.ca",
  version := "3.1.2",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
  scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies ++= Seq("chisel3","chisel-iotesters").map {
    dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) },
  unmanagedBase := (unmanagedBase in root).value,
  javacOptions ++= javacOptionsVersion(scalaVersion.value)
)

// need a separate project for placing macros
lazy val coreMacros = (project in file("./coreMacros")).
  settings(commonSettings: _*).
  // Prevent separate JARs from being generated for coreMacros.
  settings(skip in publish := true)

lazy val coherenceSettings = Seq (
  name := "chisel-coherence",
  javacOptions ++= javacOptionsVersion(scalaVersion.value),
  libraryDependencies ++= Seq(
    "edu.berkeley.cs" %% "chiseltest" % "0.2-SNAPSHOT-LOCAL",
    "uwaterloo.caesr" % "riscv" % "3.1.2",
  )
)



lazy val root = RootProject(file("."))

lazy val chiselCoherence = (project in file(".")).
  settings(commonSettings: _*).
  settings(coherenceSettings: _*).
  dependsOn(coreMacros % "compile-internal;test-internal").
  aggregate(coreMacros).
  // We used to have to disable aggregation in general in order to suppress
  //  creation of subproject JARs (coreMacros and chiselFrontend) during publishing.
  // This had the unfortunate side-effect of suppressing coverage tests and scaladoc generation in subprojects.
  // The "skip in publish := true" setting in subproject settings seems to be
  //   sufficient to suppress subproject JAR creation, so we can restore
  //   general aggregation, and thus get coverage tests and scaladoc for subprojects.
  settings(
    scalacOptions in Test ++= Seq("-language:reflectiveCalls"),
    scalacOptions in Compile in doc ++= Seq(
      "-sourcepath", (baseDirectory in ThisBuild).value.toString,
    ),
    // Include macro classes, resources, and sources main JAR since we don't create subproject JARs.
    mappings in (Compile, packageBin) ++= (mappings in (coreMacros, Compile, packageBin)).value,
    mappings in (Compile, packageSrc) ++= (mappings in (coreMacros, Compile, packageSrc)).value,
    // Export the packaged JAR so projects that depend directly on Chisel project (rather than the
    // published artifact) also see the stuff in coreMacros and chiselFrontend.
    // exportJars := true
  )

