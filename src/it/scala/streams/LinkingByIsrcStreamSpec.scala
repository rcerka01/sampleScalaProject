package streams

import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.mock.Mockito
import top.top1.therealtuesdayweld.client.{AppleClient, SecondaryMusicSegment, SegmentsDbClient, SpotifyClient}
import top.top1.therealtuesdayweld.client.tupac.TupacClient
import top.top1.therealtuesdayweld.streams.LinkingByIsrcStream

import scala.concurrent.duration._
import util.AbstractSpec
import scala.concurrent.{Await, Future}

class LinkingByIsrcStreamSpec extends AbstractSpec
  with Mockito {
  sequential

  implicit val system = ActorSystem("test" + serviceName)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val testSegmentsDbClient: SegmentsDbClient = mock[SegmentsDbClient]
  val testTupacClient: TupacClient = mock[TupacClient]
  val testSpotifyClient: SpotifyClient = mock[SpotifyClient]
  val testAppleClient: AppleClient = mock[AppleClient]

  val testLinkingStream = new LinkingByIsrcStream(testTupacClient, testSpotifyClient, testAppleClient, testSegmentsDbClient)

  private def runStreamOnce= {
    val resultF = testLinkingStream.createStream().take(1).runWith(Sink.ignore)
    Await.result(resultF, 10.seconds)
  }

  "Linking by ISRC Stream" should {
    "update Apple and Spotify links if they not exist in DB" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = None,
          appleUrl = None))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { Some("spotify_link") }
      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { Some("apple_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_link")), any, ===(false)) returns Future(1)
      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_link")), ===(false)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_link"))) returns Future(1)
      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update Apple and Spotify links if they exist in DB as fallback links" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = Some("spotify_fallback"),
          appleUrl = Some("apple_fallback"),
          isFallback = Some(true)))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { Some("spotify_updated_link") }
      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { Some("apple_updated_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_updated_link")), any, ===(false)) returns Future(1)
      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_updated_link")), ===(false)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_updated_link"))) returns Future(1)
      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_updated_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update fallback status from 'true' to 'false' īf Apple or Spotify links do not exist for a specific ISRC" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = None,
          appleUrl = None,
          isFallback = Some(true)))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { None }
      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { None }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, any, ===(false)) returns Future(1)

      runStreamOnce

      ok
    }

    "update fallback status from 'NULL' to 'false' īf Apple or Spotify links do not exist for a specific ISRC" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = None,
          appleUrl = None,
          isFallback = None))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { None }
      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { None }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, any, ===(false)) returns Future(1)

      runStreamOnce

      ok
    }

    "update Apple and Spotify links if ISRC tag change" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = Some("spotify_prev"),
          appleUrl = Some("apple_prev")))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { Some("spotify_new") }
      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { Some("apple_new") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_new")), any, ===(false)) returns Future(1)
      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_new")), ===(false)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_new"))) returns Future(1)
      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_new"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update only Spotify link if Apple already exists in DB" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = None,
          appleUrl = Some("apple_link")))
      }

      testAppleClient.getUrlByIsrc(===("isrc")) returns Future { Some("apple_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), ===(Some("spotify_link")), any, ===(false)) returns Future(1)

      testTupacClient.updateAppleLink(===("track_id"), ===(Some("apple_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "update only Apple link if Spotify already exists in DB" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = Some("spotify_link"),
          appleUrl = None))
      }

      testSpotifyClient.getUrlByIsrc(===("isrc")) returns Future { Some("spotify_link") }

      testSegmentsDbClient.upsertMusicLinks(===("track_id"), ===("seg_1"), any, ===(Some("apple_link")), ===(false)) returns Future(1)

      testTupacClient.updateSpotifyLink(===("track_id"), ===(Some("spotify_link"))) returns Future(1)

      runStreamOnce

      ok
    }

    "do not update Spotify link in DB if there is no upstream response and it is not in DB" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = None,
          appleUrl = Some("apple_link")))
      }

      testSpotifyClient.getUrlByIsrc("isrc") returns Future { None }

      testAppleClient.getUrlByIsrc("isrc") returns Future { Some("apple_link") }

      runStreamOnce

      ok
    }

    "do not update Apple link in DB if there is no upstream response and it is not in DB" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = Some("spotify_link"),
          appleUrl = None))
      }

      testSpotifyClient.getUrlByIsrc("isrc") returns Future { Some("spotify_link") }

      testAppleClient.getUrlByIsrc("isrc") returns Future { None }

      runStreamOnce

      ok
    }

    "delete Apple or Spotify links from DB if there is no upstream response" in {
      testSegmentsDbClient.getTracksByIsrc returns Future {
        Seq(SecondaryMusicSegment(
          musicId = Some("track_id"),
          segmentId = Some("seg_1"),
          isrcId = "isrc",
          spotifyUrl = Some("spotify_fallback"),
          appleUrl = Some("apple_fallback")))
      }

      testSpotifyClient.getUrlByIsrc("isrc") returns Future { None }

      testAppleClient.getUrlByIsrc("isrc") returns Future { None }

      testSegmentsDbClient.deleteAppleLink(===("track_id"), ===("seg_1"), ===(false)) returns Future(1)
      testSegmentsDbClient.deleteSpotifyLink(===("track_id"), ===("seg_1"), ===(false)) returns Future(1)

      testTupacClient.deleteMusicLink(===("track_id"), ===(true), any) returns Future(1)
      testTupacClient.deleteMusicLink(===("track_id"), any, ===(true)) returns Future(1)

      runStreamOnce

      ok
    }
  }
}
