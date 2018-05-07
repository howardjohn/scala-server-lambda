package io.github.howardjohn.lambda.http4s.example

import cats.effect.IO
import io.github.howardjohn.lambda.http4s.Http4sLambdaHandler
import org.http4s.HttpService
import org.http4s.circe.jsonOf
import io.circe.generic.auto._
import org.http4s.dsl.io._

object Route {
  implicit val inputDecoder = jsonOf[IO, Input]

  // Set up the route
  val service: HttpService[IO] = HttpService[IO] {
    case GET -> Root / "hello" / name :? TimesQueryMatcher(times) =>
      Ok(s"Hello, $name." * times.getOrElse(1))
    case req @ POST -> Root / "message" =>
      for {
        inp <- req.as[Input]
        resp <- Ok(inp.messages.map(msg => s"$msg, ${inp.name}!").mkString(" "))
      } yield resp
  }

  // Define the entry point for Lambda
  // Referenced as ` io.github.howardjohn.http4s.lambda.example$Route::handler` in Lambda
  class EntryPoint extends Http4sLambdaHandler(service)

  case class Input(
    name: String,
    messages: Seq[String]
  )

  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")
}
