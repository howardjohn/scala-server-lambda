package io.github.howardjohn.http4s.lambda

import cats.effect.IO
import fs2.{text, Stream}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.{EmptyBody, Header, Headers, Method, ParseFailure, Request, Uri}

object Encoding {

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

  def decodeRequest(rawInput: String): Either[Exception, Request[IO]] =
    for {
      input <- decode[ProxyRequest](rawInput)
      request <- parseRequest(input)
    } yield request

  def encodeResponse(response: ProxyResponse): String =
    response.asJson.noSpaces

  private def parseRequest(request: ProxyRequest): Either[ParseFailure, Request[IO]] =
    for {
      uri <- Uri.fromString(reconstructPath(request))
      method <- Method.fromString(request.httpMethod)
    } yield
      Request[IO](
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

  private def reconstructPath(request: ProxyRequest): String = {
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
