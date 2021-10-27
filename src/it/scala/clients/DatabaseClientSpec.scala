package clients

import FutureSupport
import org.specs2.mock.Mockito
import util.AbstractSpec

class DatabaseClientSpec extends AbstractSpec with Mockito with FutureSupport {

  sequential

  "Database client" should {
    "not return fallbacks if at least one record have ISRC" in new AbstractScope {

      insertSegment(
        musicId = "test_id_1",
        isrc = Some("isrc_1"),
        artist = "artist_1",
        title = "title_1"
      )

      insertSegment(
        musicId = "test_id_1",
        isrc = None,
        artist = "artist_2",
        title = "title_2"
      )

      whenReady(segmentsTestDbClient.getTracksByArtistAndName) { tracks =>
        tracks.length must be equalTo 0
      }
    }

    "not return fallbacks items if 'is_editorial' field is 'true'" in new AbstractScope {

      insertTrack(
        musicId = "test_id_1",
        isrc = None,
        apple = "apple_link_1",
        spotify = "spotify_link_2",
        artist = "artist_1",
        title = "title_1",
        isEditorial = true
      )

      whenReady(segmentsTestDbClient.getTracksByArtistAndName) { tracks =>
        tracks.length must be equalTo 0
      }
    }

    "not return search by ISRC items if 'is_editorial' field is 'true'" in new AbstractScope {

      insertTrack(
        musicId = "test_id_1",
        isrc = Some("isrc_1"),
        apple = "apple_link_1",
        spotify = "spotify_link_2",
        artist = "artist_1",
        title = "title_1",
        isEditorial = true)

      whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        tracks.length must be equalTo 0
      }
    }

    "return tracks only when ISRC tag is present" in new AbstractScope {

      insertTrack(
        musicId = "test_id_1",
        isrc = Some("isrc_1"),
        apple = "apple_link_1",
        spotify = "spotify_link_2",
        artist = "artist_1",
        title = "title_1"
      )

      insertTrack(
        musicId = "test_id_2",
        isrc = None,
        apple = "apple_link_2",
        spotify = "spotify_link_2",
        artist = "artist_2",
        title = "title_2"
      )

      whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        tracks.length must be equalTo 1
        tracks.head.musicId.get must be equalTo "test_id_1"
      }
    }

    "return tracks oly with type Music or Classical" in new AbstractScope {

      insertSegment(
        musicId = "test_id_1",
        isrc = Some("isrc_1"),
        artist = "artist_1",
        title = "title_1",
        musicType = "music",
        segmentVersionId = Some("v1"),
        versionId = Some("v1")
      )

      insertSegment(
        musicId = "test_id_2",
        isrc = Some("isrc_2"),
        artist = "artist_2",
        title = "title_2",
        musicType = "classical",
        segmentVersionId = Some("v2"),
        versionId = Some("v2")
      )

      insertSegment(
        musicId = "test_id_3",
        isrc = Some("isrc_3"),
        artist = "artist_3",
        title = "title_3",
        musicType = "speech",
        segmentVersionId = Some("v3"),
        versionId = Some("v3")
      )

      whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        tracks.length must be equalTo 2
      }
    }

    "does not return a track which has no version" in new AbstractScope {

      insertSegment(
        musicId = "test_id_1",
        isrc = Some("isrc_1"),
        artist = "artist_1",
        title = "title_1",
        versionId = Some("not_equal_to_segment_version_id"),
        segmentVersionId = Some("not_equal_to_version_id")
      )

      whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        tracks.length must be equalTo 0
      }
    }

    "deletle Apple link" in new AbstractScope {
      insertTrack(
        musicId = "test_id_1",
        segmentId = "seg_2",
        isrc = Some("isrc_1"),
        apple = "apple_link_1",
        spotify = "spotify_link_1",
        artist = "artist_1",
        title = "title_1",
        isFallback = false
      )

      whenReady(segmentsTestDbClient.deleteAppleLink("test_id_1", "seg_2", isFallback = false)) { response =>

        whenReady(segmentsTestDbClient.deleteAppleLink("test_id_1", "seg_2", isFallback = false)) { response =>
          whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
            response must be equalTo 1
            tracks.length must be equalTo 1
            tracks.head.appleUrl must be equalTo None
            tracks.head.spotifyUrl must be equalTo Some("spotify_link_1")
          }
        }
      }

      "deletle Spotify link" in new AbstractScope {
        insertTrack(
          musicId = "test_id_1",
          segmentId = "seg_1",
          isrc = Some("isrc_1"),
          apple = "apple_link_1",
          spotify = "spotify_link_1",
          artist = "artist_1",
          title = "title_1",
          isFallback = true
        )

        whenReady(segmentsTestDbClient.deleteSpotifyLink("test_id_1", "seg_1", isFallback = true)) { response =>
          whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
            response must be equalTo 1
            tracks.length must be equalTo 1
            tracks.head.spotifyUrl must be equalTo None
            tracks.head.appleUrl must be equalTo Some("apple_link_1")
          }
        }
      }

      /** todo when decided in memory DB
        **
        *"update Apple link" in new AbstractScope {
        * insertTrack(
        * musicId = "test_id_1",
        * isrc = Some("isrc_1"),
        * apple = "apple_link_1",
        * spotify = "spotify_link_1",
        * artist = "artist_1",
        * title = "title_1"
        * )
        * whenReady(segmentsTestDbClient.upsertMusicLinks(musicId = "test_id_1", apple = Some("updated_apple_link_1"))) { response =>
        * whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        * response must be equalTo 1
        *tracks.head.appleUrl.head must be equalTo "updated_apple_link_1"
        * }
        * }
        * }
        **
        *"update Spotify link" in new AbstractScope {
        * insertTrack(
        * musicId = "test_id_1",
        * isrc = Some("isrc_1"),
        * apple = "apple_link_1",
        * spotify = "spotify_link_1",
        * artist = "artist_1",
        * title = "title_1"
        * )
        **
        *whenReady(segmentsTestDbClient.upsertMusicLinks(musicId = "test_id_1", spotify = Some("updated_spotify_link_1"))) { response =>
        * whenReady(segmentsTestDbClient.getTracksByIsrc) { tracks =>
        * response must be equalTo 1
        *tracks.head.spotifyUrl.head must be equalTo "updated_spotify_link_1"
        * }
        * }
        * }
        *
        * */
    }
  }
}
