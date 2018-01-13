package howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import io.circe.parser.decode
import io.circe.Json
import io.circe.syntax._
import io.circe.parser.parse
import io.circe.generic.auto._

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Encoding {

  sealed trait HttpRequest

  case class Get(
    url: String,
    headers: Option[Map[String, String]]
  ) extends HttpRequest

  case class Post(
    url: String,
    body: Json,
    headers: Option[Map[String, String]]
  ) extends HttpRequest

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
      request <- parseRequest(input)
    } yield request
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

  private def parseRequest(request: ProxyRequest): Try[HttpRequest] =
    request.httpMethod match {
      case "GET" => Success(Get(reconstructPath(request), request.headers))
      case "POST" =>
        for {
          body <- parse(request.body.getOrElse("")).toTry
        } yield Post(reconstructPath(request), body, request.headers) // todo keep body optional?
      case _ => Failure(new RuntimeException("Invalid proxy request"))
    }

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
