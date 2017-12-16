scalaVersion := "2.12.3"

val Http4sVersion = "0.18.0-M8"

lazy val http4slambda = project
  .in(file("http4s-lambda"))
  .settings(
    moduleName := "http4s-lambda",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-client" % Http4sVersion,
      "io.circe" %% "circe-parser" % "0.9.0",
      "io.circe" %% "circe-generic" % "0.9.0",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    )
  )

lazy val example = project
  .in(file("example"))
  .enablePlugins(AwsLambdaPlugin)
  .settings(
    moduleName := "example",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-parser" % "0.9.0",
      "io.circe" %% "circe-generic" % "0.9.0"
    ),
    lambdaHandlers := Seq("test" -> "howardjohn.http4s.lambda.example.Route$EntryPoint::handle"),
    s3Bucket := Some("http4s4lambda-jars"),
    awsLambdaMemory := Some(512),
    awsLambdaTimeout := Some(10),
    roleArn := Some("arn:aws:iam::415308607406:role/service-role/lamda"),
    region := Some("us-west-2"),
    vpcConfigSecurityGroupIds := None,
    vpcConfigSubnetIds := None
  )
  .dependsOn(http4slambda)

scalacOptions ++= Seq("-Ypartial-unification")
