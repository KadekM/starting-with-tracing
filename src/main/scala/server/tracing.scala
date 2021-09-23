package server

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration, SenderConfiguration}
import zio.telemetry.opentracing.*
import zio.*
import zio.clock.*

private val sampler = new SamplerConfiguration()
  .withType("const")
  .withParam(1)

private val sender = new SenderConfiguration()
  .withAgentHost("127.0.0.1")
  .withAgentPort(6831)

private val reporter = new ReporterConfiguration()
  .withLogSpans(true)
  .withSender(sender)

def serviceTracer(serviceName: String): URLayer[Clock, OpenTracing] =
  val tracer = Configuration(serviceName)
    .withSampler(sampler)
    .withReporter(reporter)
    .getTracer

  OpenTracing.live(tracer)
