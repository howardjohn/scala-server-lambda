package howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Headers}

import scala.io.Source
import scala.util.Try

object Encoding {

  case class HttpRequest(
    method: String,
    url: String,
    body: Option[String],
    headers: Headers
  )

  case class ProxyRequest(
    path: String,
    httpMethod: String,
    body: Option[String],
    headers: Option[Map[String, String]],
    queryStringParameters: Option[Map[String, String]]
  )

  case class ProxyResponse(
    statusCode: Int,
    headers: Map[String, String],
    body: String
  )

  def in(is: InputStream): Try[HttpRequest] = {
    val t = for {
      rawInput <- Try(Source.fromInputStream(is).mkString)
      input <- decode[ProxyRequest](rawInput).toTry
    } yield parseRequest(input)
    is.close()
    t
  }

  def out(output: ProxyResponse, os: OutputStream): Try[Unit] = {
    val t = Try {
      os.write(output.asJson.noSpaces.getBytes("UTF-8"))
    }
    os.close()
    t
  }

  private def parseRequest(request: ProxyRequest): HttpRequest =
    HttpRequest(
      request.httpMethod,
      reconstructPath(request),
      request.body,
      Headers(
        request.headers
          .getOrElse(Map())
          .map {
            case (k, v) => Header.Raw(CaseInsensitiveString(k), v)
          }
          .toList)
    )

  private def reconstructPath(request: ProxyRequest): String = {
    val requestString = request.queryStringParameters
      .map {
        _.map {
          case (k, v) => s"$k=$v"
        }.mkString("?", "&", "")
      }
      .getOrElse("")
    request.path + requestString
  }
}
