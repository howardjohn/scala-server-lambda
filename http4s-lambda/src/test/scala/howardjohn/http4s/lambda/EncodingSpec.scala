package howardjohn.http4s.lambda
import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import howardjohn.http4s.lambda.Encoding._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{Header, Headers}
import org.scalatest.TryValues._
import org.scalatest._

class EncodingSpec extends FlatSpec with Matchers {
  import EncodingSpec._
  "in" should "map empty values" in {
    runReq(ProxyRequest("GET", "/api", None, None, None)).shouldBe(
      HttpRequest("GET", "/api", Headers.empty, None)
    )
  }

  it should "combine url and empty query parameters" in {
    runReq(ProxyRequest("GET", "/api", None, None, Some(Map()))).shouldBe(
      HttpRequest("GET", "/api", Headers.empty, None)
    )
  }

  it should "combine url and one query parameter" in {
    runReq(ProxyRequest("GET", "/api", None, None, Some(Map("a" -> "b")))).shouldBe(
      HttpRequest("GET", "/api?a=b", Headers.empty, None)
    )
  }

  it should "combine url and many query parameters" in {
    runReq(ProxyRequest("GET", "/api", None, None, Some(Map("a" -> "b", "c" -> "d")))).shouldBe(
      HttpRequest("GET", "/api?a=b&c=d", Headers.empty, None)
    )
  }

  it should "map headers" in {
    runReq(ProxyRequest("GET", "/api", Some(Map("a" -> "b", "c" -> "d")), None, None)).shouldBe(
      HttpRequest("GET", "/api", Headers(Header("a", "b"), Header("c", "d")), None)
    )
  }

  it should "map body" in {
    runReq(ProxyRequest("GET", "/api", None, Some("body"), None)).shouldBe(
      HttpRequest("GET", "/api", Headers.empty, Some("body"))
    )
  }
}

object EncodingSpec {

  def toStream(source: String): InputStream =
    new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8))

  def runReq(req: ProxyRequest): HttpRequest =
    in(toStream(req.asJson.noSpaces)).success.value

}
