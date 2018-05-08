package io.github.howardjohn.lambda

import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

object ProxyEncoding {
  case class ProxyRequest(
    httpMethod: String,
    path: String,
    headers: Option[Map[String, String]],
    body: Option[String],
    queryStringParameters: Option[Map[String, String]]
  )

  case class ProxyResponse(
    statusCode: Int,
    headers: Map[String, String],
    body: String
  )

  def parseRequest(rawInput: String): Either[circe.Error, ProxyRequest] = decode[ProxyRequest](rawInput)

  def encodeResponse(response: ProxyResponse): String =
    response.asJson.noSpaces

  def reconstructPath(request: ProxyRequest): String = {
    val requestString = request.queryStringParameters
      .map {
        _.map {
          case (k, v) => s"$k=$v"
        }.mkString("&")
      }
      .map { qs =>
        if (qs.isEmpty) "" else "?" + qs
      }
      .getOrElse("")
    request.path + requestString
  }
}
