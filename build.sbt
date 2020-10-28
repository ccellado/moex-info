import scala.sys.process._

val buildFrontend: TaskKey[Unit] = taskKey[Unit]("building frontend")
val compileSass: TaskKey[Unit] = taskKey[Unit]("compiling sass")
val shell: Seq[String] = {
  if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c")
  else Seq("bash", "-c")
}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """moex-play-test""",
    version := "0.1",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      guice,
      ws,
      "com.typesafe.play" %% "play-slick" % "5.0.0",
      "com.typesafe.slick" %% "slick" % "3.3.3",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
      "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
      "com.propensive" %% "magnolia" % "0.17.0",
      "org.scala-lang" % "scala-reflect" % "2.13.0"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    ),
    buildFrontend := {
      val s: TaskStreams = streams.value
      val yarnBuild: Seq[String] =
        shell :+ "cd components/multiselect && yarn build 2> >(grep -v warning 1>&2)"
      s.log.info("building frontend...")
      yarnBuild !!
    },
    compileSass := {
      val s: TaskStreams = streams.value
      val sassCompile: Seq[String] =
        shell :+ "cd components/sass && sass --sourcemap=none main.sass:../../public/stylesheets/main.css"
      s.log.info("compiling sass...")
      sassCompile !!
    },
    run := ((run in Compile) dependsOn compileSass).evaluated,
    compile := ((compile in Compile) dependsOn compileSass).value,
    run := ((run in Compile) dependsOn buildFrontend).evaluated,
    compile := ((compile in Compile) dependsOn buildFrontend).value
  )
