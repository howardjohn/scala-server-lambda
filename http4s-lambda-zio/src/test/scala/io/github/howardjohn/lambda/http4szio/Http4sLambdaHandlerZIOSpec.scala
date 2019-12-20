package io.github.howardjohn.lambda.http4szio

import io.circe.generic.auto._
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import io.github.howardjohn.lambda.http4s.TestRoutes
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.scalatest.{FeatureSpec, GivenWhenThen}
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

class Http4sLambdaHandlerZIOSpec extends FeatureSpec with LambdaHandlerBehavior with GivenWhenThen {
  implicit val jsonDecoder: EntityDecoder[Task, JsonBody] = jsonOf[Task, JsonBody]

  val handler = new Http4sLambdaHandlerZIO(new TestRoutes[Task].routes)

  scenariosFor(behavior(handler))
}
