package io.github.howardjohn.lambda.http4s

import cats.{Applicative, MonadError}
import cats.effect.Sync
import cats.implicits._
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Header, HttpRoutes}
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

class TestRoutes[F[_]] {

  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")

  val dsl = Http4sDsl[F]

  import dsl._

  def routes(implicit sync: Sync[F],
             jsonDecoder: EntityDecoder[F, JsonBody],
             me: MonadError[F, Throwable],
             stringDecoder: EntityDecoder[F, String],
             ap: Applicative[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "hello" :? TimesQueryMatcher(times) =>
      Ok {
        Seq
          .fill(times.getOrElse(1))("Hello World!")
          .mkString(" ")
      }
    case GET -> Root / "long" => Applicative[F].pure(Thread.sleep(1000)).flatMap(_ => Ok("Hello World!"))
    case GET -> Root / "exception" => throw RouteException()
    case GET -> Root / "error" => InternalServerError()
    case req@GET -> Root / "header" =>
      val header = req.headers.find(h => h.name.value == inputHeader).map(_.value).getOrElse("Header Not Found")
      Ok(header, Header(outputHeader, outputHeaderValue))
    case req@POST -> Root / "post" => req.as[String].flatMap(s => Ok(s))
    case req@POST -> Root / "json" => req.as[JsonBody].flatMap(s => Ok(LambdaHandlerBehavior.jsonReturn.asJson))
  }

}
