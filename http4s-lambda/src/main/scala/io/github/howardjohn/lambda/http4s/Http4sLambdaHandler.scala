package io.github.howardjohn.lambda.http4s

import cats.effect.IO
import fs2.{text, Stream}
import io.github.howardjohn.lambda.ProxyEncoding._
import io.github.howardjohn.lambda.{LambdaHandler, ProxyEncoding}
import org.http4s._

import scala.util.Try

class Http4sLambdaHandler(service: HttpRoutes[IO]) extends LambdaHandler {
  import Http4sLambdaHandler._

  override def handleRequest(request: ProxyRequest): ProxyResponse =
    parseRequest(request)
      .map(runRequest)
      .flatMap(_.attempt.unsafeRunSync())
      .fold(errorResponse, identity)

  private def runRequest(request: Request[IO]): IO[ProxyResponse] =
    Try {
      service
        .run(request)
        .getOrElse(Response.notFound)
        .flatMap(asProxyResponse)
    }.fold(errorResponse.andThen(e => IO(e)), identity)
}

private object Http4sLambdaHandler {
  private val errorResponse = (err: Throwable) => ProxyResponse(500, Map.empty, err.getMessage)

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
      uri <- Uri.fromString(ProxyEncoding.reconstructPath(request))
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
