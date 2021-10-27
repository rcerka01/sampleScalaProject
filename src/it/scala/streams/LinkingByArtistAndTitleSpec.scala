package streams

import org.specs2.mock.Mockito
import top.top1.therealtuesdayweld.client.{AppleClient, FallbackMusicSegment, SegmentsDbClient, SpotifyClient}
import top.top1.therealtuesdayweld.client.tupac.TupacClient
import top.top1.therealtuesdayweld.streams.LinkingByArtistAndNameStream

import util.AbstractSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LinkingByArtistAndTitleSpec extends AbstractSpec
  with Mockito {
  sequential

  implicit val system = ActorSystem("test" + serviceName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val testSegmentsDbClient: SegmentsDbClient = mock[SegmentsDbClient]
  val testTupacClient: TupacClient = mock[TupacClient]
  val testSpotifyClient: SpotifyClient = mock[SpotifyClient]
  val testAppleClient: AppleClient = mock[AppleClient]

  val testLinkingStream = new LinkingByArtistAndNameStream(testTupacClient, testSpotifyClient, testAppleClient, testSegmentsDbClient)

  private def runStreamOnce= {
    val resultF = testLinkingStream.createStream().take(1).runWith(Sink.ignore)
    Await.result(resultF, 10.seconds)
  }

  "Linking by Artist and Title Stream" should {
    "update Apple and Spotify links if they not exist in DB" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = None,
          appleUrl = None,
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("spotify_link") }
      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("apple_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_link")), any, ===(true)) returns Future(1)
      testSegmentsDbClient.upsertMusicLinks(===("track_id"),  ===("seg_1"), any, ===(Some("apple_link")), ===(true)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_link"))) returns Future(1)
      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update Apple and Spotify links if they exist in DB but artist or title are changed" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = Some("spotify_link_prev"),
          appleUrl = Some("apple_link_prev"),
          artist = Some("artist_new"),
          creditRole = Some("PERFORMER"),
          title = Some("title_new")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist_new"), ===("title_new")) returns Future { Some("spotify_link_new") }

      testAppleClient.getUrlByArtistAndTitle(===("artist_new"), ===("title_new")) returns Future { Some("apple_link_new") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_link_new")), any, ===(true)) returns Future(1)
      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_link_new")), ===(true)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_link_new"))) returns Future(1)
      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_link_new"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update fallback status from 'false' to 'true' for empty response" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title"),
          spotifyUrl = None,
          appleUrl = None,
          isFallback = Some(false)))
      }

      testSpotifyClient.getUrlByArtistAndTitle(artist = "artist", title = "title") returns Future { None }
      testAppleClient.getUrlByArtistAndTitle(artist = "artist", title = "title") returns Future { None }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, any, ===(true)) returns Future(1)

      runStreamOnce

      ok
    }

    "update fallback status from 'NULL' to 'true' for empty response" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title"),
          spotifyUrl = None,
          appleUrl = None,
          isFallback = None))
      }

      testSpotifyClient.getUrlByArtistAndTitle(artist = "artist", title = "title") returns Future { None }
      testAppleClient.getUrlByArtistAndTitle(artist = "artist", title = "title") returns Future { None }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, any, ===(true)) returns Future(1)

      runStreamOnce

      ok
    }

    "update only Spotify link if Apple already exists in DB" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = None,
          appleUrl = Some("apple_link"),
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("spotify_link") }

      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("apple_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_link")), any, ===(true)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update only Apple link if Spotify already exists in DB" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = Some("spotify_link"),
          appleUrl = None,
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("apple_link") }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("spotify_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_link")), ===(true)) returns Future(1)

      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "do not update Spotify link in DB if there is no upstream response and it is not in DB" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = None,
          appleUrl = Some("apple_link"),
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { None }

      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("apple_link") }

      runStreamOnce

      ok
    }

    "do not update Apple link in DB if there is no upstream response and it is not in DB" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = Some("spotify_link"),
          appleUrl = None,
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { Some("spotify_link") }

      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { None }

      runStreamOnce

      ok
    }

    "delete Apple or Spotify links from DB if there is no upstream response" in {
      testSegmentsDbClient.getTracksByArtistAndName returns Future {
        Seq(FallbackMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          segmentMusicId = Some("seg_1"),
          spotifyUrl = Some("spotify_link"),
          appleUrl = Some("apple_link"),
          artist = Some("artist"),
          creditRole = Some("PERFORMER"),
          title = Some("title")
        ))
      }

      testSpotifyClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { None }

      testAppleClient.getUrlByArtistAndTitle(===("artist"), ===("title")) returns Future { None }

      testSegmentsDbClient.deleteAppleLink(===("track_id"), ===("seg_1"), ===(true)) returns Future(1)
      testSegmentsDbClient.deleteSpotifyLink(===("track_id"), ===("seg_1"), ===(true)) returns Future(1)

      testTupacClient.deleteMusicLink(===("track_id"), ===(true), any) returns Future(1)
      testTupacClient.deleteMusicLink(===("track_id"), any, ===(true)) returns Future(1)

      runStreamOnce

      ok
    }
  }
}
