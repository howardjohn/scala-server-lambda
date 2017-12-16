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
        case Get(url) => GET(asUri(url))
        case Post(url, body) => POST(asUri(url), body)
      }
      .map(_.unsafeRunSync)
      .map(runRequest)
      .flatMap(result => out(result, os))
      .get

  private def runRequest(request: Request[IO]): String =
    service
      .run(request)
      .getOrElse(Response.notFound)
      .flatMap(_.as[String])
      .unsafeRunSync()
}

object LambdaHandler {

  def asUri(uri: String): Uri = Uri.unsafeFromString(uri)

}
