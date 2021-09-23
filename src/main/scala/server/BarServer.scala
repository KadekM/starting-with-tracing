package server

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration, SenderConfiguration}
import io.opentracing.propagation.TextMapAdapter
import zhttp.http.{Request as ZioRequest, Response as ZioResponse, *}
import zhttp.service.Server
import zio.*
import zio.telemetry.opentracing.*
import server.*
import server.FooServerApp.RequestBackend
import sttp.*
import io.opentracing.propagation
import org.apache.http.HttpHost
import org.elasticsearch.client.*

import scala.jdk.CollectionConverters.*

object BarServerApp extends App:
  val tracer = serviceTracer("bar")

  val esClient = EsThinClient.liveLocalhost

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server
      .start(9000, BarServer.api)
      .provideCustomLayer(tracer ++ esClient)
      .exitCode

object BarServer:
  val api = Http.collectM[ZioRequest] { case req @ Method.POST -> Root / "bar" =>
    val headers = req.headers.map(x => x.name.toString -> x.value.toString).toMap.asJava
    val response = for
      esClient <- ZIO.service[EsThinClient]
      _        <- zio.console.putStrLn("bar received message ")
      _        <- ZIO.foreachPar_(1 to 3)(_ => esClient.clusterHealth).span("make es requests")
    yield ZioResponse.text("bar response")

    val span = s"${req.method.toString()} ${req.url.asString}"

    response
      .spanFrom(propagation.Format.Builtin.TEXT_MAP, new TextMapAdapter(headers), span)
  }
