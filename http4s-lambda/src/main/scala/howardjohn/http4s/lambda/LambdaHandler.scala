package howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import cats.effect.IO
import howardjohn.http4s.lambda.Encoding._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.dsl.io._

class LambdaHandler(service: HttpService[IO]) {
  import LambdaHandler._

  def handle(is: InputStream, os: OutputStream): Unit =
    in(is)
      .map {
        case Get(url, headers) => GET(asUri(url))
        case Post(url, body, headers) => POST(asUri(url), body)
      }
      .map(_.unsafeRunSync) // todo bring this out with for loop, map
      .map(runRequest)
      .flatMap(result => out(result, os))
      .get

  private def runRequest(request: Request[IO]): ProxyResponse =
    service
      .run(request)
      .getOrElse(Response.notFound)
      .flatMap(asProxyResponse)
      .unsafeRunSync()

  private def asProxyResponse(resp: Response[IO]): IO[ProxyResponse] =
    for {
      body <- resp.as[String]
    } yield ProxyResponse(
      resp.status.code,
      resp.headers
        .map(h => h.name.value -> h.value)
        .toMap,
      body)
}

object LambdaHandler {

  def asUri(uri: String): Uri = Uri.unsafeFromString(uri)

}
