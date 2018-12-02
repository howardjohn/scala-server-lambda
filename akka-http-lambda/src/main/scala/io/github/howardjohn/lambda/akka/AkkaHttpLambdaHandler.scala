package io.github.howardjohn.lambda.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.github.howardjohn.lambda.ProxyEncoding._
import io.github.howardjohn.lambda.{LambdaHandler, ProxyEncoding}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class AkkaHttpLambdaHandler(route: Route)(
  implicit system: ActorSystem,
  materializer: ActorMaterializer,
  ec: ExecutionContext
) extends LambdaHandler {
  import AkkaHttpLambdaHandler._

  override def handleRequest(request: ProxyRequest): ProxyResponse =
    Await.result(runRequest(proxyToAkkaRequest(request)), Duration.Inf)

  private def runRequest(request: HttpRequest): Future[ProxyResponse] = {
    val source = Source.single(request)
    val sink = Sink.head[HttpResponse]
    source
      .via(route)
      .toMat(sink)(Keep.right)
      .run()
      .flatMap(asProxyResponse)
  }

  private def proxyToAkkaRequest(request: ProxyRequest): HttpRequest =
    new HttpRequest(
      method = parseHttpMethod(request.httpMethod),
      uri = Uri(ProxyEncoding.reconstructPath(request)),
      headers = parseRequestHeaders(request.headers.getOrElse(Map.empty)),
      entity = parseEntity(request.headers.getOrElse(Map.empty), request.body),
      protocol = HttpProtocols.`HTTP/1.1`
    )

  private def parseEntity(headers: Map[String, String], body: Option[String]): MessageEntity = {
    val defaultContentType = ContentTypes.`text/plain(UTF-8)`
    val contentType = ContentType
      .parse(headers.getOrElse("Content-Type", defaultContentType.value))
      .getOrElse(defaultContentType)

    body match {
      case Some(b) => HttpEntity(contentType, b.getBytes)
      case None => HttpEntity.empty(contentType)
    }
  }

  private def asProxyResponse(resp: HttpResponse): Future[ProxyResponse] =
    Unmarshal(resp.entity)
      .to[String]
      .map { body =>
        ProxyResponse(
          resp.status.intValue(),
          resp.headers.map(h => h.name -> h.value).toMap,
          body
        )
      }
}

private object AkkaHttpLambdaHandler {
  private def parseRequestHeaders(headers: Map[String, String]): List[HttpHeader] =
    headers.map {
      case (k, v) =>
        HttpHeader.parse(k, v) match {
          case ParsingResult.Ok(header, _) => header
          case ParsingResult.Error(err) => throw new RuntimeException(s"Failed to parse header $k:$v with error $err.")
        }
    }.toList

  private def parseHttpMethod(method: String) = method.toUpperCase match {
    case "CONNECT" => HttpMethods.CONNECT
    case "DELETE" => HttpMethods.DELETE
    case "GET" => HttpMethods.GET
    case "HEAD" => HttpMethods.HEAD
    case "OPTIONS" => HttpMethods.OPTIONS
    case "PATCH" => HttpMethods.PATCH
    case "POST" => HttpMethods.POST
    case "PUT" => HttpMethods.PUT
    case "TRACE" => HttpMethods.TRACE
    case other => HttpMethod.custom(other)
  }
}
