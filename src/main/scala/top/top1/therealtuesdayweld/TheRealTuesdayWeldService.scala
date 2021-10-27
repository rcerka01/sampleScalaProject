package top.top1.therealtuesdayweld

import client.monitoring.Monitoring
import org.slf4s.Logging
import top.top1.therealtuesdayweld.api.StatusApi
import top.top1.therealtuesdayweld.config.Config
import top.top1.therealtuesdayweld.streams.{LinkingByArtistAndNameStream, LinkingByIsrcStream}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

object TheRealTuesdayWeldService extends App
  with StatusApi
  with Monitoring
  with Logging
  with Config {

  val logger = Logging(system, getClass)
  logger.info(s"Starting $serviceName on $httpInterface:$httpPort")

  val decider: Supervision.Decider = {
    case e: Throwable =>
      recordException(e.getClass.getSimpleName)
      log.error(s"Decider: '${e.getClass}' - Exception received: ${e.getMessage}")
      Supervision.Restart
  }

  val settings = ActorMaterializerSettings(system)
    .withSupervisionStrategy(decider)

  implicit def executor: ExecutionContextExecutor = system.dispatcher

  implicit val materialize: ActorMaterializer = ActorMaterializer(settings)

  Kamon.start()

  val linkingStreamByIsrc = new LinkingByIsrcStream(tupacClient, spotifyClient, appleClient, segmentsDbClient)
  val linkingStreamByArtistAndName = new LinkingByArtistAndNameStream(tupacClient, spotifyClient, appleClient, segmentsDbClient)

  if (linkingByIsrcStreamEnabled) {
    linkingStreamByIsrc.createStream().runWith(Sink.ignore)
      .onComplete {
        case Success(_) => log.info(s"Linking by isrc stream completed with success")
        case Failure(e) => log.error(s"Linking by isrc stream completed with failure ${e.getMessage}")
      }
  }

  if (linkingByArtistAndTitleStreamEnabled) {
    linkingStreamByArtistAndName.createStream().runWith(Sink.ignore)
      .onComplete {
        case Success(_) => log.info(s"Linking by artist and title stream completed with success")
        case Failure(e) => log.info(s"Linking by artist and title stream completed with failure: ${e.getMessage}")
      }
  }

  Http().bindAndHandle(statusRoutes, httpInterface, httpPort)

  log.info(s"Starting $serviceName on $httpInterface:$httpPort")
}
