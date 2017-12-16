package howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}

import io.circe.parser.decode
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._

import scala.io.Source
import scala.util.Try

object Encoding {

  sealed trait HttpMethod
  case class Get(url: String) extends HttpMethod
  case class Post(url: String, body: Json) extends HttpMethod

  def in(is: InputStream): Try[HttpMethod] = {
    val t = for {
      str <- Try(Source.fromInputStream(is).mkString)
      input <- decode[HttpMethod](str).toTry
    } yield input
    is.close()
    t
  }

  def out(output: String, os: OutputStream): Try[Unit] = {
    val t = Try {
      os.write(output.asJson.noSpaces.getBytes("UTF-8"))
    }
    os.close()
    t
  }
}
