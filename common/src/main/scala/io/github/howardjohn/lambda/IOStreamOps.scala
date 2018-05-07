package io.github.howardjohn.lambda

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import scala.io.Source

object StreamOps {
  implicit class InputStreamOps(val is: InputStream) extends AnyVal {
    def consume(): String = {
      val contents = Source.fromInputStream(is).mkString
      is.close()
      contents
    }
  }

  implicit class OutputStreamOps(val os: OutputStream) extends AnyVal {
    def writeAndClose(contents: String): Unit = {
      os.write(contents.getBytes(StandardCharsets.UTF_8))
      os.close()
    }
  }
}
