package io.github.howardjohn.lambda.http4s

import cats.{Applicative, MonadError}
import io.github.howardjohn.lambda.{LambdaHandler, ProxyEncoding}
import io.github.howardjohn.lambda.ProxyEncoding.{ProxyRequest, ProxyResponse}
import org.http4s._
import fs2.{Pure, Stream, text}
import cats.implicits._

import scala.util.Try

trait Http4sLambdaHandlerK[F[_]] extends LambdaHandler {
  val service: HttpRoutes[F]

  def handleRequest(request: ProxyRequest): ProxyResponse

  def runRequest(request: Request[F])(implicit F: MonadError[F, Throwable],
                                      decoder: EntityDecoder[F, String]): F[ProxyResponse] =
    Try {
      service
        .run(request)
        .getOrElse(Response.notFound)
        .flatMap(asProxyResponse)
    }.fold(errorResponse.andThen(e => Applicative[F].pure(e)), identity)

  protected val errorResponse = (err: Throwable) => ProxyResponse(500, Map.empty, err.getMessage)

  protected def asProxyResponse(resp: Response[F])(implicit F: MonadError[F, Throwable],
                                                   decoder: EntityDecoder[F, String]): F[ProxyResponse] =
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

  protected def parseRequest(request: ProxyRequest): Either[ParseFailure, Request[F]] =
    for {
      uri <- Uri.fromString(ProxyEncoding.reconstructPath(request))
      method <- Method.fromString(request.httpMethod)
    } yield
      Request[F](
        method,
        uri,
        headers = request.headers.map(toHeaders).getOrElse(Headers.empty),
        body = request.body.map(encodeBody).getOrElse(EmptyBody)
      )

  protected def toHeaders(headers: Map[String, String]): Headers =
    Headers {
      headers.map {
        case (k, v) => Header(k, v)
      }.toList
    }

  protected def encodeBody(body: String): Stream[Pure, Byte] = Stream(body).through(text.utf8Encode)
}

