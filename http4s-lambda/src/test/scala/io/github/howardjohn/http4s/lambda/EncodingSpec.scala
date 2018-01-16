package io.github.howardjohn.http4s.lambda

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.http4s.lambda.Encoding._
import org.http4s.Uri.uri
import org.http4s._
import org.scalatest._

class EncodingSpec extends FlatSpec with Matchers {
  import EncodingSpec._
  "decodeRequest" should "map empty values" in {
    val req = runReq(ProxyRequest("GET", "/api", None, None, None))
    assert(req.headers == Headers.empty)
    assert(req.body == EmptyBody)
  }

  it should "map http method" in {
    val req = runReq(ProxyRequest("GET", "/api", None, None, None))
    assert(req.method == Method.GET)
  }

  it should "map body" in {
    val req = runReq(ProxyRequest("GET", "/api", None, Some("body"), None))
    assert(req.body.compile.toList.unsafeRunSync == "body".getBytes.toSeq)
  }

  it should "combine url and empty query parameters" in {
    val req = runReq(ProxyRequest("GET", "/api", None, None, Some(Map())))
    assert(req.uri == uri("/api"))
  }

  it should "combine url and one query parameter" in {
    val req = runReq(ProxyRequest("GET", "/api", None, None, Some(Map("a" -> "b"))))
    assert(req.uri == uri("/api?a=b"))
  }

  it should "combine url and many query parameters" in {
    val req = runReq(ProxyRequest("GET", "/api", None, None, Some(Map("a" -> "b", "c" -> "d"))))
    assert(req.uri == uri("/api?a=b&c=d"))
  }

  it should "map headers" in {
    val req = runReq(ProxyRequest("GET", "/api", Some(Map("a" -> "b", "c" -> "d")), None, None))
    assert(req.method == Method.GET)
    assert(req.headers == Headers(Header("a", "b"), Header("c", "d")))
  }
}

object EncodingSpec {

  def runReq(req: ProxyRequest): Request[IO] =
    decodeRequest(req.asJson.noSpaces).right.get

}
