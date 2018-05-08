package io.github.howardjohn.lambda

import java.io.{InputStream, OutputStream}

import io.github.howardjohn.lambda.ProxyEncoding._
import io.github.howardjohn.lambda.StreamOps._

trait LambdaHandler {
  def handleRequest(request: ProxyRequest): ProxyResponse

  def handle(is: InputStream, os: OutputStream): Unit = {
    val rawInput = is.consume()
    val request = parseRequest(rawInput).fold(
      e => throw e,
      identity
    )
    val rawResponse = handleRequest(request)
    val response = encodeResponse(rawResponse)
    os.writeAndClose(response)
  }
}
