# http4s-lambda
http4s-lambda allows you to run http4s `HttpServices` over API Gateway and AWS Lambda.

## Getting Started

First, we define a simple `HttpService`. Then, we simply need to define a new class for Lambda.

```scala
object Route {
  // Set up the route
  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name!")
  }
  
  // Define the entry point for Lambda
  class EntryPoint extends LambdaHandler(service)
}
```

The set up for `EntryPoint` is somewhat strange, but due to Lambda instantiating and instance with reflection, this seems to be the simplest declaration.

Once deployed to Lambda, the handler should be specified as `<package>.Route$EntryPoint::handler`.

Finally, an API can be created in API Gateway. [Lambda Proxy integration](https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html) must be enabled.

A complete example can be found in [here](example). Note the use of [serverless](https://github.com/serverless/serverless), which automates deployment of the package and sets up API Gateway, along with many other features.

