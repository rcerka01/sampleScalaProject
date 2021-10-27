package top.top1.therealtuesdayweld.streams

import scala.concurrent.{ExecutionContextExecutor, Future}
import org.slf4s.Logging
import top.top1.therealtuesdayweld.client.{AppleClient, SecondaryMusicSegment, SegmentsDbClient, SpotifyClient}
import top.top1.therealtuesdayweld.client.tupac.TupacClient

import scala.concurrent.duration._

class LinkingByIsrcStream(
                           val tupacClient: TupacClient,
                           spotifyClient: SpotifyClient,
                           appleClient: AppleClient,
                           val segmentsDbClient: SegmentsDbClient)
                         (implicit val system: ActorSystem,
                          override implicit val executionContext: ExecutionContextExecutor)
  extends Stream
    with DatabaseAndTupacInteractions
    with Monitoring
    with Logging {

  val tracksSource: Source[Seq[SecondaryMusicSegment], NotUsed] = Source.repeat(1).mapAsync(1) { _ =>
    segmentsDbClient.getTracksByIsrc
  }

  val updateAppleAndSpotify: Flow[SecondaryMusicSegment, Unit, NotUsed] = {
    Flow[SecondaryMusicSegment].mapAsync(1) { track => {

      track.musicId flatMap { musicId =>
        track.segmentId map { segmentId =>

          val apple = {
            increment(appleSearchByIsrc)
            timedFuture("get-apple-search-by-isrc") {
              appleClient.getUrlByIsrc(track.isrcId)
            }
          }
          val spotify = {
            increment(spotifySearchByIsrc)
            timedFuture("get-spotify-search-by-isrc") {
              spotifyClient.getUrlByIsrc(track.isrcId)
            }
          }

          apple flatMap { newAppleLink =>
            spotify flatMap { newSpotifyLink =>

              val backfill = backfillByIsrc
              /**
                * APPLE
                */

              val appleAction = getActionForService(backfill, track.appleUrl, newAppleLink, track.isFallback, Apple)
              val appleUrl = getUrlFromAction(appleAction)

              /**
                * SPOTIFY
                * --"--
                */
              val spotifyAction: Action = getActionForService(backfill, track.spotifyUrl, newSpotifyLink, track.isFallback, Spotify)
              val spotifyUrl = getUrlFromAction(spotifyAction)


              updateServiceLinkInDatabaseThenTupac(musicId, segmentId, spotifyAction, Spotify, spotifyUrl, false)
                .onComplete {
                  _ => updateServiceLinkInDatabaseThenTupac(musicId, segmentId, appleAction, Apple, appleUrl, false)
                }

              Future.successful {
                ()
              }
            }
          }
        }
      }
    }.getOrElse(throw new Exception("Track ID not exists.")) }
  }

  def createStream(): Source[Unit, NotUsed] = {
    tracksSource
      .via(throttle(throttleLinkingByIsrcStreamInSeconds.seconds))
      .via(oneAtATime)
      .via(throttleByOne(throttleStreamByOneInSeconds.milliseconds))
      .via(updateAppleAndSpotify)
  }

}
