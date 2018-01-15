package io.github.howardjohn.http4s.lambda.example

import cats.effect.IO
import io.circe.generic.auto._
import io.github.howardjohn.http4s.lambda.LambdaHandler
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._

object Route {
  implicit val InputDecoder = jsonOf[IO, Input]

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
  class EntryPoint extends LambdaHandler(service)

  case class Input(
    name: String,
    messages: Seq[String]
  )

  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")
}
