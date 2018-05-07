package io.github.howardjohn.lambda.http4s

import cats.effect.{Effect, IO}
import fs2.{text, Stream}
import io.github.howardjohn.lambda.Encoding._
import io.github.howardjohn.lambda.{Encoding, LambdaHandler}
import org.http4s._

class Http4sLambdaHandler(service: HttpService[IO]) extends LambdaHandler {
  import Http4sLambdaHandler._

  override def handleRequest(request: ProxyRequest): ProxyResponse =
    parseRequest(request)
      .map(runRequest)
      .fold(
        err => throw err,
        response => response.unsafeRunSync()
      )

  private def runRequest(request: Request[IO]): IO[ProxyResponse] =
    service
      .run(request)
      .getOrElse(Response.notFound)
      .flatMap(asProxyResponse)

}

private object Http4sLambdaHandler {
  private def asProxyResponse(resp: Response[IO]): IO[ProxyResponse] =
    resp
      .as[String]
      .map { body =>
        ProxyResponse(
          resp.status.code,
          resp.headers
            .map(h => h.name.value -> h.value)
            .toMap,
          body)
      }

  private def parseRequest(request: ProxyRequest): Either[ParseFailure, Request[IO]] =
    for {
      uri <- Uri.fromString(Encoding.reconstructPath(request))
      method <- Method.fromString(request.httpMethod)
    } yield
      Request[IO](
        method,
        uri,
        headers = request.headers.map(toHeaders).getOrElse(Headers.empty),
        body = request.body.map(encodeBody).getOrElse(EmptyBody)
      )

  private def toHeaders(headers: Map[String, String]): Headers =
    Headers {
      headers.map {
        case (k, v) => Header(k, v)
      }.toList
    }

  private def encodeBody(body: String) = Stream(body).through(text.utf8Encode)
}
