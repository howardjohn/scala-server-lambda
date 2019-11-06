package io.github.howardjohn.lambda.http4s

import cats.effect.IO
import io.github.howardjohn.lambda.ProxyEncoding._
import org.http4s._

case class Http4sLambdaHandler(service: HttpRoutes[IO]) extends Http4sLambdaHandlerK[IO] {
  def handleRequest(request: ProxyRequest): ProxyResponse =
    parseRequest(request)
      .map(runRequest)
      .flatMap(_.attempt.unsafeRunSync())
      .fold(errorResponse, identity)
}
