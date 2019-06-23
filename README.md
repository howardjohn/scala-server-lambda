# Scala Servers for Lambda
Scala Servers for Lambda allows you to run existing Scala servers over API Gateway and AWS Lambda.

Benefits:
* Define logic once, and use it in both a server and serverless environment.
* Simpler testing, as the logic has been isolated from Lambda completely.
* No need to deal with Lambda's API directly, which aren't easy to use from Scala.
* All code is deployed to single Lambda function, meaning our functions will be kept warm more often.

## Supported Servers

* [http4s](http://http4s.org) - version 0.20
* [akka-http](https://doc.akka.io/docs/akka-http/current) - version 10.1.1

## Dependencies

Having a large JAR can increase cold start times, so dependencies have been kept to a minimum. All servers depend on [circe](https://circe.github.io/circe/). Additionally:

* http4s depends on `http4s-core`.
* akka-http depends on `akka-http` and `akka-stream`.

Neither of these depend on the AWS SDK at all, which substantially reduces the size.

## Getting Started

More thorough examples can be found in the examples directory.

### http4s

First, add the dependency:

```scala
libraryDependencies += "io.github.howardjohn" %% "http4s-lambda" % "0.3.1"
```

Next, we define a simple `HttpService`. Then, we simply need to define a new class for Lambda.

```scala
object Route {
  // Set up the route
  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name!")
  }

  // Define the entry point for Lambda
  class EntryPoint extends Http4sLambdaHandler(service)
}
```

Thats it! Make sure any dependencies are initialized in the Route object so they are computed only once.


### akka-http

First, add the dependency:

```scala
libraryDependencies += "io.github.howardjohn" %% "akka-http-lambda" % "0.3.1"
```

Next, we define a simple `Route`. Then, we simply need to define a new class for Lambda.

```scala
object Route {
  // Set up the route
  val route: Route =
    path("hello" / Segment) { name: String =>
      get {
        complete(s"Hello, $name!")
      }
    }

  // Set up dependencies
  implicit val system: ActorSystem = ActorSystem("example")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  // Define the entry point for Lambda
  class EntryPoint extends AkkaHttpLambdaHandler(route)
}
```

Thats it! Make sure any dependencies are initialized in the Route object so they are computed only once.

## Deploying to AWS

To deploy to Lambda, we need to create a jar with all of our dependencies. The easiest way to do this is using [sbt-assembly](https://github.com/sbt/sbt-assembly).

Once we have the jar, all we need to do is upload it to Lambda. The preferred way to do this is using the [serverless](https://github.com/serverless/serverless) framework which greatly simplifies this (and is what is used in the examples), but there is no issues with doing it manually.

When deploying to Lambda, the handler should be specified as `<PACKAGE_NAME>.Route$EntryPoint::handle` (if you followed the example above).

Finally, an API can be created in API Gateway. [Lambda Proxy integration](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html) must be enabled.

## Versions

| Version      | http4s Version | akka-http Version |
|--------------|----------------|-------------------|
| 0.4          | 0.20           | 10.1              |
| 0.3.1        | 0.18           | 10.1              |
