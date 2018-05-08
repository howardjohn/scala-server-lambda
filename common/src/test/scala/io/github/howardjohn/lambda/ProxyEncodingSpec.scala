package io.github.howardjohn.lambda

import io.github.howardjohn.lambda.ProxyEncoding._
import org.scalatest.FlatSpec

class ProxyEncodingSpec extends FlatSpec {
  import ProxyEncodingSpec._

  "reconstructPath" should "handle no parameters" in
    assert {
      reconstructPath(makeRequest(query = Some(Map.empty))) === path
    }

  it should "handle null parameters" in
    assert {
      reconstructPath(makeRequest(query = None)) === path
    }

  it should "handle one parameter" in
    assert {
      reconstructPath(makeRequest(query = Some(Map("param1" -> "value1")))) === s"$path?param1=value1"
    }

  it should "handle two parameters" in {
    val params = Map("param1" -> "value1", "param2" -> "value2")
    assert {
      reconstructPath(makeRequest(query = Some(params))) === s"$path?param1=value1&param2=value2"
    }
  }

  "parseRequest" should "handle just a method and path" in {
    val raw = makeRawRequest(makeRequest())
    assert {
      parseRequest(raw) === Right(makeRequest())
    }
  }

  it should "handle just a query parameters" in {
    val request = makeRequest(query = Some(Map("param1" -> "option1")))
    val raw = makeRawRequest(request)
    assert {
      parseRequest(raw) === Right(request)
    }
  }

  it should "handle multiple query parameters" in {
    val request = makeRequest(query = Some(Map("param1" -> "option1", "param2" -> "option2")))
    val raw = makeRawRequest(request)
    assert {
      parseRequest(raw) === Right(request)
    }
  }

  it should "handle a body" in {
    val request = makeRequest(body = Some("some body"))
    val raw = makeRawRequest(request)
    assert {
      parseRequest(raw) === Right(request)
    }
  }
}

object ProxyEncodingSpec {
  val path = "www.example.com/"

  def makeRawRequest(request: ProxyRequest): String =
    s"""
      |{
      |  "httpMethod": "${request.httpMethod}",
      |  "path": "${request.path}",
      |  "body": ${request.body.map(b => "\"" + b + "\"").getOrElse("null")},
      |  "headers": ${request.headers.map(_.toSeq).map(pairsToJson).getOrElse("null")},
      |  "queryStringParameters": ${request.queryStringParameters.map(_.toSeq).map(pairsToJson).getOrElse("null")}
      |}
    """.stripMargin

  private def pairsToJson(pairs: Seq[(String, String)]): String = {
    val body = pairs
      .map {
        case (k, v) => s""""$k": "$v""""
      }
      .mkString(",\n")
    s"{\n$body\n}"
  }

  def makeRequest(
    httpMethod: String = "GET",
    path: String = path,
    headers: Option[Map[String, String]] = None,
    body: Option[String] = None,
    query: Option[Map[String, String]] = None
  ) = ProxyRequest(
    httpMethod,
    path,
    headers,
    body,
    query
  )
}
