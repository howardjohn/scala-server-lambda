package howardjohn.http4s.lambda.example

import cats.effect._
import howardjohn.http4s.lambda.LambdaHandler
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object Route {
  case class Input(
    name: String,
    messages: Seq[String]
  )
  implicit val InputDecoder = jsonOf[IO, Input]
  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")

  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "hello" / name :? TimesQueryMatcher(times) =>
      Ok(s"Hello, $name." * times.getOrElse(1))
    case req @ POST -> Root / "message" =>
      for {
        inp <- req.as[Input]
        resp <- Ok(inp.messages.map(msg => s"$msg, ${inp.name}!").mkString(" "))
      } yield resp
  }

  class EntryPoint extends LambdaHandler(service)
}
