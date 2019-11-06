package io.github.howardjohn.lambda.http4szio

import fs2.{Stream, text}
import io.github.howardjohn.lambda.ProxyEncoding._
import io.github.howardjohn.lambda.{LambdaHandler, ProxyEncoding}
import org.http4s._
import zio._
import zio.interop.catz._

import scala.util.Try

class Http4sLambdaHandlerZIO(service: HttpRoutes[Task]) extends LambdaHandler {
  import Http4sLambdaHandlerZIO._

  val runtime: DefaultRuntime = new DefaultRuntime {}

  override def handleRequest(request: ProxyRequest): ProxyResponse =
    parseRequest(request)
      .map(runRequest)
      .flatMap(request => runtime.unsafeRun(request.either))
      .fold(errorResponse, identity)

  private def runRequest(request: Request[Task]): Task[ProxyResponse] =
    Try {
      service
        .run(request)
        .getOrElse(Response.notFound)
        .flatMap(asProxyResponse)
    }.fold(errorResponse.andThen(e => IO(e)), identity)
}

private object Http4sLambdaHandlerZIO {
  private val errorResponse = (err: Throwable) => ProxyResponse(500, Map.empty, err.getMessage)

  private def asProxyResponse(resp: Response[Task]): Task[ProxyResponse] =
    resp
      .as[String]
      .map { body =>
        ProxyResponse(
          resp.status.code,
          resp.headers.toList
            .map(h => h.name.value -> h.value)
            .toMap,
          body)
      }

  private def parseRequest(request: ProxyRequest): Either[ParseFailure, Request[Task]] =
    for {
      uri <- Uri.fromString(ProxyEncoding.reconstructPath(request))
      method <- Method.fromString(request.httpMethod)
    } yield
      Request[Task](
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
