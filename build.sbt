val akkaVersion = "2.5.26"
val korolevVersion = "0.14.0"
val commonSettings = Seq(
  scalacOptions ++= Seq("-Yrangepos", "-deprecation"),
  organization := "com.tenderowls",
  version      := "1.0.0-SNAPSHOT",
  scalaVersion := "2.13.1"
)

lazy val match3 = project
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies += "org.specs2" %% "specs2-core" % "4.8.0" % Test
  )

lazy val server = project
  .settings(commonSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
    )
  )
  .dependsOn(match3)

lazy val client = project
  .enablePlugins(UniversalPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(DockerPlugin)
  .settings(commonSettings:_*)
  .settings(
    packageName in Docker := "match3",
    version in Docker := "1.0.0",
    maintainer in Docker := "Aleksey Fomkin <aleksey.fomkin@gmail.com>",
    dockerExposedPorts := Seq(8080),
    dockerUsername := Some("fomkin"),
    dockerUpdateLatest := true,
    normalizedName := "match3-client",
    libraryDependencies ++= Seq(
      "com.github.fomkin" %% "korolev-server-akkahttp" % korolevVersion,
      "org.slf4j" % "slf4j-simple" % "1.7.+"
    )
  )
  .dependsOn(server)

lazy val root = project.in(file("."))
  .aggregate(match3, server, client)
