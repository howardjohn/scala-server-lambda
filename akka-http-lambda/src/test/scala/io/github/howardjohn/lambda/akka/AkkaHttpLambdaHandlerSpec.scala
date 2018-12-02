package io.github.howardjohn.lambda.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.github.howardjohn.lambda.LambdaHandlerBehavior
import io.github.howardjohn.lambda.LambdaHandlerBehavior._
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

import scala.concurrent.Future

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
    } ~ path("json") {
      post {
        import CirceJsonMarshalling._
        import io.circe.generic.auto._
        entity(as[JsonBody]) { entity =>
          complete(LambdaHandlerBehavior.jsonReturn.asJson)
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

/**
 * Provides (un)marshallers for akka-http that are about the json content type
 */
object CirceJsonMarshalling {
  implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.stringUnmarshaller
      .forContentTypes(`application/json`)
      .flatMap { ctx => mat => json =>
        decode[A](json).fold(Future.failed, Future.successful)
      }

  implicit final def marshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(`application/json`) { a =>
      HttpEntity(`application/json`, a.asJson.noSpaces)
    }
}
