package io.github.howardjohn.lambda.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

class AkkaHttpLambdaHandlerSpec
    extends FeatureSpec
    with LambdaHandlerBehavior
    with GivenWhenThen
    with BeforeAndAfterAll {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route: Route =
    path("hello") {
      (get & parameter("times".as[Int] ? 1)) { times =>
        complete {
          Seq
            .fill(times)("Hello World!")
            .mkString(" ")
        }
      }
    } ~ path("long") {
      get {
        Thread.sleep(1000)
        complete("")
      }
    } ~ path("post") {
      post {
        entity(as[String]) { body =>
          complete(body)
        }
      }
    } ~ path("exception") {
      get {
        throw RouteException()
      }
    } ~ path("error") {
      get {
        complete(StatusCodes.InternalServerError)
      }
    } ~ path("header") {
      get {
        headerValueByName(inputHeader) { header =>
          respondWithHeaders(RawHeader(outputHeader, outputHeaderValue)) {
            complete(header)
          }
        }
      }
    }

  val handler = new AkkaHttpLambdaHandler(route)

  scenariosFor(behavior(handler))

  override def afterAll(): Unit = {
    materializer.shutdown()
    system.terminate()
  }
}
