package io.github.howardjohn.lambda.http4szio

import io.circe.generic.auto._
import io.circe.syntax._
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.http4s.{EntityDecoder, Header, HttpRoutes}
import org.scalatest.{FeatureSpec, GivenWhenThen}
import zio._
import zio.duration._
import zio.interop.catz._
import zio.interop.catz.implicits._

class Http4sLambdaHandlerZIOSpec extends FeatureSpec with LambdaHandlerBehavior with GivenWhenThen {
  implicit val jsonDecoder: EntityDecoder[Task, JsonBody] = jsonOf[Task, JsonBody]

  object TimesQueryMatcher extends OptionalQueryParamDecoderMatcher[Int]("times")

  val dsl = Http4sDsl[Task]
  import dsl._

  val route: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "hello" :? TimesQueryMatcher(times) =>
      Ok {
        Seq
          .fill(times.getOrElse(1))("Hello World!")
          .mkString(" ")
      }
    case GET -> Root / "long" => Task(ZIO.sleep(1000.milliseconds)).flatMap(_ => Ok("Hello World!"))
    case GET -> Root / "exception" => throw RouteException()
    case GET -> Root / "error" => InternalServerError()
    case req @ GET -> Root / "header" =>
      val header = req.headers.find(h => h.name.value == inputHeader).map(_.value).getOrElse("Header Not Found")
      Ok(header, Header(outputHeader, outputHeaderValue))
    case req @ POST -> Root / "post" => req.as[String].flatMap(s => Ok(s))
    case req @ POST -> Root / "json" => req.as[JsonBody].flatMap(s => Ok(LambdaHandlerBehavior.jsonReturn.asJson))
  }

  val handler = new Http4sLambdaHandlerZIO(route)

  scenariosFor(behavior(handler))
}
