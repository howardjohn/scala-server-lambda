lazy val commonSettings = Seq(
  organization := "io.github.howardjohn",
  scalaVersion := "2.12.6"
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .aggregate(common, http4s, exampleHttp4s)

lazy val CirceVersion = "0.9.0"
lazy val ScalaTestVersion = "3.0.4"

lazy val common = project
  .in(file("common"))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    moduleName := "common",
    libraryDependencies ++=
      Seq(
        "io.circe" %% "circe-generic" % CirceVersion,
        "io.circe" %% "circe-parser" % CirceVersion,
        "org.scalatest" %% "scalatest" % ScalaTestVersion % "test"
      )
  )

lazy val http4s = project
  .in(file("http4s-lambda"))
  .settings(publishSettings)
  .settings(commonSettings)
  .settings(
    name := "http4s-lambda",
    version := "0.2.0",
    moduleName := "http4s-lambda",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= {
      val Http4sVersion = "0.18.10"
      Seq(
        "org.http4s" %% "http4s-core" % Http4sVersion,
        "org.http4s" %% "http4s-circe" % Http4sVersion,
        "io.circe" %% "circe-parser" % CirceVersion,
        "io.circe" %% "circe-generic" % CirceVersion,
        "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
        "org.http4s" %% "http4s-dsl" % Http4sVersion % "test"
      )
    }
  )
  .dependsOn(common)

lazy val exampleHttp4s = project
  .in(file("example-http4s"))
  .settings(noPublishSettings)
  .settings(commonSettings)
  .settings(
    moduleName := "example-http4s",
    assemblyJarName in assembly := "example-http4s.jar",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % "0.18.0-M8"
    )
  )
  .dependsOn(http4s)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/howardjohn/http4s-lambda")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/howardjohn/http4s-lambda"),
      "scm:git@github.com:howardjohn/http4s-lambda.git"
    )),
  developers := List(
    Developer(
      id = "howardjohn",
      name = "John Howard",
      email = "johnbhoward96@gmail.com",
      url = url("https://github.com/howardjohn/")
    )
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)
