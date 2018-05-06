package io.github.howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import cats.effect.IO
import io.github.howardjohn.http4s.lambda.Encoding._
import io.github.howardjohn.http4s.lambda.IOStreamOps._
import org.http4s._

class LambdaHandler(service: HttpService[IO]) {

  def handle(is: InputStream, os: OutputStream): Unit = {
    val result = for {
      input <- is.consume()
      request <- IO.fromEither(decodeRequest(input))
      response <- runRequest(request)
      rawResponse = encodeResponse(response)
      _ <- os.writeAndClose(rawResponse)
    } yield ()

    result.unsafeRunSync()
  }

  private def runRequest(request: Request[IO]): IO[ProxyResponse] =
    service
      .run(request)
      .getOrElse(Response.notFound)
      .flatMap(asProxyResponse)

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
}
