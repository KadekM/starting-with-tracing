package server

import org.apache.http.HttpHost
import org.elasticsearch.action.admin.cluster.health.{ClusterHealthRequest, ClusterHealthResponse}
import org.elasticsearch.client.{RequestOptions, RestClient, RestHighLevelClient}
import zio.*
import zio.blocking.*
import zio.telemetry.opentracing.*

trait EsThinClient:
  def clusterHealth: ZIO[Blocking & OpenTracing, Throwable, ClusterHealthResponse]

object EsThinClient:
  type EsThinClientService = Has[EsThinClient]

  def liveLocalhost: ZLayer[Any, Throwable, EsThinClientService] =
    val makeClient = ZIO.effect(
      new RestHighLevelClient(
        RestClient.builder(new HttpHost("localhost", 9200, "http"))
      )
    )
    ZManaged
      .fromAutoCloseable(makeClient)
      .map { restClient =>
        new EsThinClient {
          override def clusterHealth: ZIO[Blocking & OpenTracing, Throwable, ClusterHealthResponse] =
            effectBlockingIO(restClient.cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT))
              .span("health request")
        }
      }
      .toLayer
