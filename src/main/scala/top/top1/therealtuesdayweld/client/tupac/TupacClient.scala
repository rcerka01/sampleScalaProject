package top.top1.therealtuesdayweld.client.tupac

import org.slf4s.Logging
import top.top1.therealtuesdayweld.config.Config
import top.top1.therealtuesdayweld.domain.tupac.TupacResponse.MusicTrackEntryItem

import scala.concurrent.{ExecutionContext, Future}

case class TupacClientException(msg: String, status: Int) extends Exception(msg)

case class TupacResponse(data: Option[MusicTrackEntryItem])

class TupacClient(publishClient: PublishClient)(implicit val executionContext: ExecutionContext) extends SnsMessageWriter
  with Monitoring with Config with Logging {

  sealed trait LinkType { val string: String }

  case object AppleLinkType extends LinkType {
    override val string: String = "APPLE_WEB"
  }

  case object SpotifyLinkType extends LinkType {
    override val string: String = "SPOTIFY_WEB"
  }

  private def deleteOneMusicLink(musicId: String, linkType: LinkType): Future[Int] =
    publishClient.publishMessage(createDeleteSNSMessage(musicId, linkType.string))

  private def updateExternalLink(musicId: String, linkType: String, link: String) =
    publishClient.publishMessage(createUpdateSNSMessage(musicId, linkType, link))

  def updateAppleLink(musicId: String, apple: Option[String]): Future[Int] = {
    increment(appleTupacUpdates)
    updateExternalLink(musicId, AppleLinkType.string, apple.getOrElse(""))
  }
  def updateSpotifyLink(musicId: String, spotify: Option[String]): Future[Int] = {
    increment(spotifyTupacUpdates)
    updateExternalLink(musicId, SpotifyLinkType.string, spotify.getOrElse(""))
  }

  def deleteMusicLink(musicId: String, apple: Boolean = false, spotify: Boolean = false): Future[Int] =
    (apple, spotify) match {
      case (true, false) =>
        increment(appleTupacDeletes)
        deleteOneMusicLink(musicId, AppleLinkType)
      case (false, true) =>
        increment(spotifyTupacDeletes)
        deleteOneMusicLink(musicId, SpotifyLinkType)
      case (true, true) =>
        throw new IllegalArgumentException("link type can't be both Apple and Spotify, please provide only one")
      case (false, false) =>
        throw new IllegalArgumentException("link type is not given, please choose either apple or Spotify")
    }
}
