scalaVersion := "2.12.4"

val Http4sVersion = "0.18.0-M8"

lazy val http4slambda = project
  .in(file("http4s-lambda"))
  .settings(
    moduleName := "http4s-lambda",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-parser" % "0.9.0",
      "io.circe" %% "circe-generic" % "0.9.0",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    )
  )

lazy val example = project
  .in(file("example"))
  .settings(
    moduleName := "example",
    assemblyJarName in assembly := "example.jar",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % Http4sVersion
    )
  )
  .dependsOn(http4slambda)

scalacOptions ++= Seq("-Ypartial-unification")
