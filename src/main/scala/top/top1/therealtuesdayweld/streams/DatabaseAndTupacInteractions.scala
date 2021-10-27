package top.top1.therealtuesdayweld.streams

import org.slf4s.Logging
import top.top1.therealtuesdayweld.client.SegmentsDbClient
import top.top1.therealtuesdayweld.client.tupac.TupacClient

import scala.concurrent.{ExecutionContext, Future}

trait DatabaseAndTupacInteractions extends Logging with Monitoring {

  def segmentsDbClient: SegmentsDbClient
  def tupacClient: TupacClient
  implicit def executionContext: ExecutionContext

  def getActionForService(backfill: Boolean, oldLink: Option[String], newLink: Option[String], isFallback: Option[Boolean], Service: Service): Action = {
    (oldLink, newLink, isFallback) match {
      /**
        * Add in TUPAC all search results (backfill).
        */
      case (_, Some(newLinkUrl), _) if backfill => {
        UpdateAction(link = newLink, service = Service)
      }

      case (Some(oldLinkUrl), Some(newLinkUrl), any) =>

        /**
          * If link exists in DB AND is returned from 3rd party AND they are NOT equal and DB have fall back link.
          * That means that new ISRC tag is added and we now have result.
          * Link have to be added into DB and TUPAC.
          */
        if (!oldLinkUrl.equals(newLinkUrl)) {
          UpdateAction(link = newLink, service = Service)
        }

        /**
          * If link exists in DB AND is returned from 3rd party AND they are equal:
          * We are not interested in that case, it is a normal situation, and we are doing nothing.
          */
        else {
          NoAction
        }

      /**
        * If 3rd party link exists, but it is not in DB:
        * That means rather new ISRC tag is added and we now have result OR track become available in 3rd party.
        * link have to be added into DB and TUPAC.
        */
      case (None, Some(newLinkUrl), any) =>
        UpdateAction(link = newLink, service = Service)

      /**
        * If link exists in DB but is not returned from 3rd party:
        * That means the link become unavailable.
        * Link have to be remover ether from DB and TUPAC.
        */
      case (Some(oldLinkUrl), None, any) =>
        DeleteAction(service = Service)


      /**
        * If link not exists in DB and is not returned from 3rd party via ISRC search:
        * That means the 3rd party dont have that record.
        * Fallback state still have to be changed to False.
        */
      case (None, None, isFallback) =>
        if (isFallback.isEmpty || (isFallback.isDefined && !isFallback.get)) {
          UpdateAction(link = None, service = Service)
        } else {
          NoAction
        }

      case _ => {
        NoAction
      }
    }
  }

  def updateServiceLinkInDatabaseThenTupac(musicId: String, segmentId: String, action: Action, service: Service, url: Option[String], isFallback: Boolean): Future[Int] = {
    def performDbUpdate(update: UpdateAction): Future[Int] = service match {
      case Spotify => segmentsDbClient.upsertMusicLinks(musicId, segmentId, update.link, None, isFallback)
      case Apple => segmentsDbClient.upsertMusicLinks(musicId, segmentId, None, update.link, isFallback)
    }

    def performTupacUpdate: Future[Int] = service match {
      case Spotify => tupacClient.updateSpotifyLink(musicId, spotify = url)
      case Apple => tupacClient.updateAppleLink(musicId, apple = url)
    }

    def performDbDelete: Future[Int] = service match {
      case Spotify => segmentsDbClient.deleteSpotifyLink(musicId, segmentId, isFallback)
      case Apple => segmentsDbClient.deleteAppleLink(musicId, segmentId, isFallback)
    }

    def performTupacDelete: Future[Int] = service match {
      case Spotify => tupacClient.deleteMusicLink(musicId, false, true)
      case Apple => tupacClient.deleteMusicLink(musicId, true, false)
    }

    action match {
      case update: UpdateAction =>
        val dbUpdate = performDbUpdate(update)
        dbUpdate flatMap { dbResult =>
          log.info(s"Database update result: $dbResult for musicId: $musicId, fallback: $isFallback, url: $url")

          if (dbResult == 1) {
            val tupacUpdate = performTupacUpdate
            tupacUpdate map { tupacResult =>
              log.info(s"Tupac update result: $tupacResult for musicId: $musicId, fallback: $isFallback, url: $url")
              if (tupacResult == 1) { 1 }
              else { throw TupacException(s"${service} links are not updated in Tupac for 'musicId': $musicId") }
            }
          }
          else { throw DatabaseException(s"Database ${service} Links is not updated for 'musicId': $musicId") }
        }
      case _: DeleteAction =>
        val dbDelete = performDbDelete
        dbDelete flatMap { dbResult =>
          if (dbResult == 1) {
            val tupacDelete = performTupacDelete
            tupacDelete map { tupacResult =>
              if (tupacResult == 1) { 1 }
              else { throw TupacException(s"${service} links are not deleted in Tupac for 'musicId': $musicId") }
            }
          }
          else {
            increment("database-delete-failed")
            throw DatabaseException(s"Database ${service} is not deleted for 'musicId': $musicId")
          }
        }
      case NoAction => Future.successful(0)
    }
  }

  def getUrlFromAction(action: Action) = {
    action match {
      case action: UpdateAction => action.link
      case _ => None
    }
  }
}
