package io.github.howardjohn.lambda.http4s

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.{Header, HttpService}
import org.scalatest.{FeatureSpec, GivenWhenThen}

class Http4sLambdaHandlerSpec extends FeatureSpec with LambdaHandlerBehavior with GivenWhenThen {
  implicit val jsonDecoder = jsonOf[IO, JsonBody]
  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")
  val route = HttpService[IO] {
    case GET -> Root / "hello" :? TimesQueryMatcher(times) =>
      Ok {
        Seq
          .fill(times.getOrElse(1))("Hello World!")
          .mkString(" ")
      }
    case GET -> Root / "long" => IO(Thread.sleep(1000)).flatMap(_ => Ok("Hello World!"))
    case GET -> Root / "exception" => throw RouteException()
    case GET -> Root / "error" => InternalServerError()
    case req @ GET -> Root / "header" =>
      val header = req.headers.find(h => h.name.value == inputHeader).map(_.value).getOrElse("Header Not Found")
      Ok(header, Header(outputHeader, outputHeaderValue))
    case req @ POST -> Root / "post" => req.as[String].flatMap(s => Ok(s))
    case req @ POST -> Root / "json" => req.as[JsonBody].flatMap(s => Ok(LambdaHandlerBehavior.jsonReturn.asJson))
  }
  val handler = new Http4sLambdaHandler(route)
  scenariosFor(behavior(handler))
}
