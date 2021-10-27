package top.top1.therealtuesdayweld.streams

import org.slf4s.Logging
import top.top1.therealtuesdayweld.client.{AppleClient, FallbackMusicSegment, SegmentsDbClient, SpotifyClient}
import top.top1.therealtuesdayweld.client.tupac.TupacClient
import top.top1.therealtuesdayweld.util.SegmentContributorCalculator

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class LinkingByArtistAndNameStream(
                                    val tupacClient: TupacClient,
                                    spotifyClient: SpotifyClient,
                                    appleClient: AppleClient,
                                    val segmentsDbClient: SegmentsDbClient)
                                  (implicit val system: ActorSystem,
                                   override implicit val executionContext: ExecutionContextExecutor)
  extends Stream
    with SegmentContributorCalculator
    with DatabaseAndTupacInteractions
    with Monitoring
    with Logging {

  val tracksSource: Source[Seq[FallbackMusicSegment], NotUsed] = Source.repeat(1).mapAsync(1) { _ =>
    updateContributor(segmentsDbClient.getTracksByArtistAndName)
  }

  val updateAppleAndSpotify: Flow[FallbackMusicSegment, Unit, NotUsed] = {
    Flow[FallbackMusicSegment].mapAsync(1) { track => {

      track.musicId.flatMap { musicId =>
        track.segmentId.flatMap { segmentId =>
          track.artist.flatMap { artist =>
            track.title.map { title =>

              val apple = {
                increment(appleSearchByArtistAndTitle)
                timedFuture("get-apple-search-by-artist-and-title") {
                  appleClient.getUrlByArtistAndTitle(artist, title)
                }
              }
              val spotify = {
                increment(spotifySearchByArtistAndTitle)
                timedFuture("get-spotify-search-by-artist-and-title") {
                  spotifyClient.getUrlByArtistAndTitle(artist, title)
                }
              }

              apple flatMap { newAppleLink =>
                spotify flatMap { newSpotifyLink =>

                  val backfill = backfillByArtistAndName
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


                  updateServiceLinkInDatabaseThenTupac(musicId, segmentId, spotifyAction, Spotify, spotifyUrl, true)
                    .onComplete {
                      _ => updateServiceLinkInDatabaseThenTupac(musicId, segmentId, appleAction, Apple, appleUrl, true)
                    }

                  Future.successful {
                    ()
                  }
                }
              }
            }
          }}}
    }.getOrElse(throw new Exception("Track ID not exists.")) }
  }

  def createStream(): Source[Unit, NotUsed] = {
    tracksSource
      .via(throttle(throttleLinkingByArtistAndTitleStreamInSeconds.seconds))
      .via(oneAtATime)
      .via(throttleByOne(throttleStreamByOneInSeconds.milliseconds))
      .via(updateAppleAndSpotify)
  }
}
