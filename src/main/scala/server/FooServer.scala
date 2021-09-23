package server

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration, SenderConfiguration}
import io.opentracing.propagation.{Format, TextMapAdapter}
import zhttp.http.{Request as ZioRequest, Response as ZioResponse, *}
import zhttp.service.Server
import zio.*
import zio.telemetry.opentracing.*
import server.*
import server.FooServerApp.RequestBackend
import sttp.*
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.capabilities
import sttp.client3.*
import io.opentracing.propagation
import zio.telemetry.opentracing.OpenTracing

import scala.jdk.CollectionConverters.*
import scala.collection.mutable

object FooServerApp extends App:
  val tracer = serviceTracer("foo")

  type RequestBackend = SttpBackend[Task, capabilities.zio.ZioStreams & capabilities.WebSockets]
  val httpClient = AsyncHttpClientZioBackend.managed().toLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    val api = FooServer.api

    Server
      .start(8000, api)
      .provideCustomLayer(tracer ++ httpClient)
      .exitCode

object FooServer:
  val api = Http.collectM[ZioRequest] { case req @ Method.GET -> Root / "foo" =>
    val response = for
      _    <- zio.console.putStrLn("bar received message ")
      resp <- sendRequestToBar
    yield ZioResponse.text("sent")

    val span = s"${req.method.toString()} ${req.url.asString}"
    response.root(span)
  }

  private def sendRequestToBar =
    for
      client <- ZIO.service[RequestBackend]

      buffer <- UIO.succeed(
        new TextMapAdapter(mutable.Map.empty[String, String].asJava)
      )

      _ <- OpenTracing.inject(propagation.Format.Builtin.TEXT_MAP, buffer)
      tracingHeaders = buffer
        .iterator()
        .asScala
        .map(x => x.getKey -> x.getValue)
        .toMap

      request = basicRequest
        .post(uri"http://localhost:9000/bar")
        .headers(tracingHeaders)
      _ <- client.send(request)
    yield "ok"
