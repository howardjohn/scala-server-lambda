package io.github.howardjohn.lambda.akka.example

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.github.howardjohn.lambda.akka.AkkaHttpLambdaHandler

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
