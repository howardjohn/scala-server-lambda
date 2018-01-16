package io.github.howardjohn.http4s.lambda

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import cats.effect.IO

import scala.io.Source

object IOStreamOps {
  implicit class InputStreamOps(val is: InputStream) extends AnyVal {

    def consume(): IO[String] = IO {
      val contents = Source.fromInputStream(is).mkString
      is.close()
      contents
    }

  }

  implicit class OutputStreamOps(val os: OutputStream) extends AnyVal {

    def writeAndClose(contents: String): IO[Unit] = IO {
      os.write(contents.getBytes(StandardCharsets.UTF_8))
      os.close()
    }

  }
}
