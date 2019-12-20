package io.github.howardjohn.lambda.http4s

import cats.effect.IO
import io.circe.generic.auto._
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.http4s.EntityDecoder
import org.http4s.circe._
import org.scalatest.{FeatureSpec, GivenWhenThen}

class Http4sLambdaHandlerSpec extends FeatureSpec with LambdaHandlerBehavior with GivenWhenThen {
  implicit val jsonDecoder: EntityDecoder[IO, JsonBody] = jsonOf[IO, JsonBody]

  val handler = new Http4sLambdaHandler(new TestRoutes[IO].routes)

  scenariosFor(behavior(handler))
}
