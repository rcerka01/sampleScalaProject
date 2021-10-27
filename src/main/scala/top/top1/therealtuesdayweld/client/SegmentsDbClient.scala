package top.top1.therealtuesdayweld.client

import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.{ExecutionContextExecutor, Future}

case class SecondaryMusicSegment(
                                  musicId: Option[String],
                                  segmentId: Option[String],
                                  isrcId: String,
                                  spotifyUrl: Option[String],
                                  appleUrl: Option[String],
                                  isFallback: Option[Boolean] = Some(false),
                                  isEditorial: Option[Boolean] = Some(false))

case class FallbackMusicSegment(
                                 musicId: Option[String],
                                 segmentId: Option[String],
                                 segmentMusicId: Option[String],
                                 spotifyUrl: Option[String],
                                 appleUrl: Option[String],
                                 isFallback: Option[Boolean] = Some(true),
                                 isEditorial: Option[Boolean] = Some(false),
                                 artist: Option[String],
                                 creditRole: Option[String],
                                 title: Option[String])

class SegmentsDbClient(database: Database)(implicit val dispatcher: ExecutionContextExecutor) extends Config with Monitoring {

  implicit val secondaryMusicSegment = GetResult { result =>
    SecondaryMusicSegment(
      musicId = result.nextStringOption(),
      segmentId = result.nextStringOption(),
      isrcId = result.nextString(),
      spotifyUrl = result.nextStringOption(),
      appleUrl = result.nextStringOption(),
      isFallback = result.nextBooleanOption(),
      isEditorial = result.nextBooleanOption())
  }

  implicit val fallbackMusicSegment = GetResult { result =>
    FallbackMusicSegment(
      musicId = result.nextStringOption(),
      segmentId = result.nextStringOption(),
      segmentMusicId = result.nextStringOption(),
      spotifyUrl = result.nextStringOption(),
      appleUrl = result.nextStringOption(),
      isFallback = result.nextBooleanOption(),
      isEditorial = result.nextBooleanOption(),
      artist = result.nextStringOption(),
      creditRole = result.nextStringOption(),
      title = result.nextStringOption())
  }

  /**
    *
    * Following two queries have to exclude each other.
    */
  private def getTracksByArtistAndNameQuery: Future[Seq[FallbackMusicSegment]] = database.run(
    sql"""SELECT DISTINCT
           programmes.segment.record_id,
           programmes.segment.pid,
           programmes.music.segment_id,
           programmes.music.spotify_url,
           programmes.music.apple_url,
           programmes.music.is_fallback,
           programmes.music.is_editorial,
           programmes.contributor.name,
           programmes.contribution.credit_role_id,
           programmes.segment.title
         FROM programmes.segment
         LEFT JOIN programmes.music ON programmes.segment.record_id = programmes.music.music_id AND programmes.segment.pid = programmes.music.segment_id
         INNER JOIN programmes.contribution ON segment.pid = programmes.contribution.segment_id
         INNER JOIN programmes.contributor ON programmes.contribution.contributor_id = programmes.contributor.pid
         INNER JOIN programmes.segment_event ON programmes.segment.pid= programmes.segment_event.segment_id
         INNER JOIN programmes.versions ON programmes.versions.version_id = programmes.segment_event.version_id
         WHERE (segment.type = 'music' OR segment.type = 'classical') AND title IS NOT NULL AND name IS NOT NULL AND isrc_id IS NULL AND programmes.music.is_editorial IS NOT TRUE"""
      .as[FallbackMusicSegment].transactionally
  )

  def getTracksByArtistAndName: Future[Seq[FallbackMusicSegment]] = {
    increment(getTracksFromDBbyArtistAndName)
    timedFuture("get-all-tracks-by-artist-and-title") {
      getTracksByArtistAndNameQuery
    }
  }

  private def getTracksByIsrcQuery: Future[Seq[SecondaryMusicSegment]] = database.run(
    sql"""SELECT DISTINCT
            programmes.segment.record_id,
            programmes.segment.pid,
            programmes.segment.isrc_id,
            programmes.music.spotify_url,
            programmes.music.apple_url,
            programmes.music.is_fallback,
            programmes.music.is_editorial
          FROM programmes.segment
          INNER JOIN programmes.segment_event ON programmes.segment.pid = programmes.segment_event.segment_id
          INNER JOIN programmes.versions ON programmes.versions.version_id = programmes.segment_event.version_id
          LEFT JOIN programmes.music ON programmes.segment.record_id = programmes.music.music_id AND programmes.segment.pid = programmes.music.segment_id
          WHERE (segment.type = 'music' OR segment.type = 'classical') AND programmes.segment.isrc_id IS NOT NULL AND programmes.music.is_editorial IS NOT TRUE"""
      .as[SecondaryMusicSegment].transactionally
  )

  def getTracksByIsrc: Future[Seq[SecondaryMusicSegment]] = {
    increment(getTracksFromDBbyIsrc)
    timedFuture("get-all-tracks-isrcs") {
      getTracksByIsrcQuery
    }
  }

  private def deleteAppleLinkQuery(musicId: String, segmentId: String, isFallback: Boolean): Future[Int] = database.run(
    sqlu"""UPDATE programmes.music SET apple_url=NULL, is_fallback='#${isFallback.toString}'  WHERE music_id='#$musicId' AND segment_id='#$segmentId'"""
  )

  def deleteAppleLink(musicId: String, segmentId: String, isFallback: Boolean): Future[Int] = {
    increment(appleDBDeletes)
    timedFuture("delete-apple-linkin-in-db") {
      deleteAppleLinkQuery(musicId, segmentId: String, isFallback): Future[Int]
    }
  }

  private def deleteSpotifyLinkQuery(musicId: String, segmentId: String, isFallback: Boolean): Future[Int] = database.run(
    sqlu"""UPDATE programmes.music SET spotify_url=NULL, is_fallback='#${isFallback.toString}' WHERE music_id='#$musicId' AND segment_id='#$segmentId'"""
  )

  def deleteSpotifyLink(musicId: String, segmentId: String, isFallback: Boolean): Future[Int] = {
    increment(spotifyDBDeletes)
    timedFuture("delete-spotify-linkin-in-db") {
      deleteSpotifyLinkQuery(musicId, segmentId: String, isFallback): Future[Int]
    }
  }

  private def upsertMusicLinksQuery(musicId: String, segmentId: String, spotify: Option[String] = None, apple: Option[String] = None, isFallback: Boolean = false): Future[Int] = {
    val createLinks: String = (spotify, apple) match {
      case (Some(s), Some(a)) => s"(music_id, segment_id, apple_url, spotify_url, is_fallback) VALUES ('$musicId', '$segmentId', '$a', '$s', '$isFallback')"
      case (Some(s), None) => s"(music_id, segment_id, spotify_url, is_fallback) VALUES ('$musicId', '$segmentId', '$s', '$isFallback')"
      case (None, Some(a)) => s"(music_id, segment_id, apple_url, is_fallback) VALUES ('$musicId', '$segmentId', '$a', '$isFallback')"
      case _ => s""
    }

    val updateLinks: String = (spotify, apple) match {
      case (Some(s), Some(a)) => s"spotify_url='$s', apple_url='$a', is_fallback='${isFallback.toString}'"
      case (Some(s), None) => s"spotify_url='$s', is_fallback='${isFallback.toString}'"
      case (None, Some(a)) => s"apple_url='$a', is_fallback='${isFallback.toString}'"
      case _ => s""
    }

    if (spotify.isDefined || apple.isDefined) {
      database.run(
        sqlu"""INSERT INTO programmes.music #$createLinks
               ON CONFLICT (music_id, segment_id)
               DO UPDATE SET #$updateLinks"""
      )
    } else {
      Future.successful(0)
    }
  }

  def upsertMusicLinks(musicId: String, segmentId: String, spotify: Option[String] = None, apple: Option[String] = None, isFallback: Boolean = false): Future[Int] = {
    (spotify, apple) match {
      case (Some(s), Some(a)) => {
        increment(spotifyDBUpdates)
        increment(appleDBUpdates)
        upsertMusicLinksQuery(musicId, segmentId, spotify, apple, isFallback)
      }
      case (Some(s), None) => {
        increment(spotifyDBUpdates)
        timedFuture("update-spotify-linkin-in-db") {
          upsertMusicLinksQuery(musicId, segmentId, spotify, apple, isFallback)
        }
      }
      case (None, Some(a)) => {
        increment(appleDBUpdates)
        timedFuture("update-apple-linkin-in-db") {
          upsertMusicLinksQuery(musicId, segmentId, spotify, apple, isFallback)
        }
      }
      case _ => Future.successful(0)
    }
  }
}
