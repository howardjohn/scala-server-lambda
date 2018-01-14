package howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import cats.effect.IO
import fs2.{text, Stream}
import howardjohn.http4s.lambda.Encoding._
import org.http4s._

import scala.util.Try

class LambdaHandler(service: HttpService[IO]) {

  def handle(is: InputStream, os: OutputStream): Unit =
    createRequest(is)
      .map(runRequest)
      .flatMap(result => out(result, os))
      .get

  private def createRequest(is: InputStream): Try[Request[IO]] =
    for {
      input <- in(is)
      uri <- Uri.fromString(input.url).toTry
      method <- Method.fromString(input.method).toTry
    } yield
      Request[IO](
        method,
        uri,
        headers = input.headers,
        body = input.body.map(Stream(_).through(text.utf8Encode)).getOrElse(EmptyBody))

  private def runRequest(request: Request[IO]): ProxyResponse =
    service
      .run(request)
      .getOrElse(Response.notFound)
      .flatMap(asProxyResponse)
      .unsafeRunSync()

  private def asProxyResponse(resp: Response[IO]): IO[ProxyResponse] =
    for {
      body <- resp.as[String]
    } yield
      ProxyResponse(
        resp.status.code,
        resp.headers
          .map(h => h.name.value -> h.value)
          .toMap,
        body)
}
