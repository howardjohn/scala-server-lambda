package io.github.howardjohn.http4s.lambda

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.charset.StandardCharsets

import cats.effect.IO
import io.github.howardjohn.http4s.lambda.Encoding._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.scalatest.{FlatSpec, Matchers}

class LambdaHandlerSpec extends FlatSpec with Matchers {
  import LambdaHandlerSpec._
  "handle" should "return the body with needed headers" in {
    val service: HttpService[IO] = HttpService[IO] {
      case _ => Ok("response")
    }

    val response = doHandle(new LambdaHandler(service), ProxyRequest("GET", "/", None, None, None))
    assert(response.body == "response")
    assert(response.statusCode == 200)
  }

  it should "handle POST body" in {
    case class Input(
      data: Seq[String],
    )
    implicit val InputDecoder = jsonOf[IO, Input]
    val service: HttpService[IO] = HttpService[IO] {
      case req @ POST -> Root =>
        for {
          inp <- req.as[Input]
          resp <- Ok(inp.data.head)
        } yield resp
    }

    val response =
      doHandle(new LambdaHandler(service), ProxyRequest("POST", "/", None, Some("""{"data":["a","b"]}"""), None))
    assert(response.body == "a")
  }

  it should "return not found on a route miss" in {
    val service: HttpService[IO] = HttpService[IO] {
      case GET -> Root / "api" => Ok("Success")
    }

    val response = doHandle(new LambdaHandler(service), ProxyRequest("GET", "/", None, None, None))
    assert(response.statusCode == 404)
  }
}

object LambdaHandlerSpec {

  def toStream(source: String): InputStream =
    new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))

  def doHandle(handler: LambdaHandler, input: ProxyRequest): ProxyResponse =
    doHandle(handler, input.asJson.noSpaces)

  def doHandle(handler: LambdaHandler, input: String): ProxyResponse = {
    val os = new ByteArrayOutputStream
    handler.handle(toStream(input), os)
    decode[ProxyResponse](new String(os.toByteArray, "UTF-8")).right.get
  }
}
