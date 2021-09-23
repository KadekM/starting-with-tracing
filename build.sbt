val scala3Version = "3.0.2"

val zioHttpV = "1.0.0.0-RC17"
val openTracingV = "0.33.0"
val jaegerV = "1.6.0"
val zioTelemetryV = "0.8.2"
val log4jV = "2.14.1"
val sttpV = "3.3.14"
val elasticV = "7.15.0"

inThisBuild(Seq(
  scalaVersion := scala3Version,
  fork := true,
  libraryDependencies ++= Seq(
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jV,
    "io.d11" %% "zhttp" % zioHttpV,
    "dev.zio" %% "zio-opentracing" % zioTelemetryV,
    "io.opentracing" % "opentracing-api" % openTracingV,
    "io.jaegertracing" % "jaeger-core" % jaegerV,
    "io.jaegertracing" % "jaeger-thrift" % jaegerV,

    "com.softwaremill.sttp.client3" %% "core" % sttpV,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpV,
    "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % elasticV
)
))

lazy val root = project
  .in(file("."))
  .settings(
    name := "start-with-tracing",
  )
