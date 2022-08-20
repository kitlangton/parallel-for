ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / organization     := "io.github.kitlangton"
ThisBuild / organizationName := "kitlangton"
ThisBuild / description      := "Automatically parallelize your for comprehensions at compile time."
ThisBuild / homepage         := Some(url("https://github.com/kitlangton/parallel-for"))

val sharedSettings = Seq(
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer(
      id = "kitlangton",
      name = "Kit Langton",
      email = "kit.langton@gmail.com",
      url = url("https://github.com/kitlangton")
    )
  ),
  scalacOptions ++= Seq("-Ywarn-unused:patvars"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 =>
        List(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
      case _ =>
        List()
    }
  },
  Compile / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 => List("-Ypartial-unification")
      case _                       => List("-Ymacro-annotations")
    }
  }
)

val zio1Version        = "1.0.13"
val zio2Version        = "2.0.1"
val zioQueryVersion    = "0.3.0-RC2"
val catsEffect3Version = "3.3.5"

lazy val root = (project in file("."))
  .aggregate(core.js, core.jvm, zio.js, zio.jvm)
  .settings(
    name               := "parallel-for",
    crossScalaVersions := Nil,
    skip / publish     := true
  )

lazy val core = (crossProject(JSPlatform, JVMPlatform) in file("core"))
  .settings(
    name := "parallel-for",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "dev.zio"      %%% "zio-test"       % zio2Version % Test,
      "dev.zio"      %%% "zio-test-sbt"   % zio2Version % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    sharedSettings
  )
  .enablePlugins(ScalaJSPlugin)

lazy val zio1 = (crossProject(JSPlatform, JVMPlatform) in file("zio1"))
  .settings(
    name := "parallel-for-zio-1",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"          % zio1Version,
      "dev.zio" %%% "zio-test"     % zio1Version % Test,
      "dev.zio" %%% "zio-test-sbt" % zio1Version % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    sharedSettings
  )
  .dependsOn(core)

lazy val zio = (crossProject(JSPlatform, JVMPlatform) in file("zio"))
  .settings(
    name := "parallel-for-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"          % zio2Version,
      "dev.zio" %%% "zio-test"     % zio2Version % Test,
      "dev.zio" %%% "zio-test-sbt" % zio2Version % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    sharedSettings
  )
  .dependsOn(core)

lazy val zioQuery = (crossProject(JSPlatform, JVMPlatform) in file("zio-query"))
  .settings(
    name := "parallel-for-zio-query",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-query"    % zioQueryVersion,
      "dev.zio" %%% "zio-test"     % zio2Version % Test,
      "dev.zio" %%% "zio-test-sbt" % zio2Version % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    sharedSettings
  )
  .dependsOn(core)

lazy val catsEffect3 = (crossProject(JSPlatform, JVMPlatform) in file("cats-effect-3"))
  .settings(
    name := "parallel-for-cats-effect-3",
    libraryDependencies ++= Seq(
      "org.typelevel"       %%% "cats-effect" % catsEffect3Version,
      "com.disneystreaming" %%% "weaver-cats" % "0.7.9" % Test
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
    sharedSettings
  )
  .dependsOn(core)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
