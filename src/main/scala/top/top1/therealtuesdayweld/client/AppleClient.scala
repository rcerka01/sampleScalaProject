package top.top1.therealtuesdayweld.client

import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class AppleClientSettings(httpClient: HttpClient, protocol: String, host: String, port: Int)

case class AppleClientException(msg: String, status: Int) extends Exception(msg)

case class AppleSongResponse(data: Option[List[Data]])

case class AppleSongs(songs: Option[AppleSongResponse])

case class AppleSearchResults(results: Option[AppleSongs])

case class Data(attributes: Option[Attributes])

case class Attributes(url: Option[String])

class AppleClient(settings: AppleClientSettings) extends Config {

  private def url(path: String): String = UrlBuilder(
    protocol = settings.protocol,
    host = settings.host,
    port = settings.port,
    path = path
  )

  private def cleanUrlParameter(parameter: String) = parameter.replace('&', ' ')

  def getUrlByIsrc(isrc: String): Future[Option[String]] = {
    settings.httpClient
      .get(url(s"/v1/catalog/gb/songs?filter[isrc]=$isrc"))
      .header(key = "Authorization", value = s"Bearer $appleClientBearerToken")
      .execute() map {
      case response if response.getStatusCode() == 200 =>
        response.as[AppleSongResponse].data.flatMap(_.headOption.flatMap(_.attributes.flatMap(_.url)))
      case response =>
        throw AppleClientException(s"Unexpected HTTP status code: ${response.getStatusCode()}", response.getStatusCode())
    }
  }

  def getUrlByArtistAndTitle(artist: String, title: String): Future[Option[String]] = {
    settings.httpClient
      .get(url(s"/v1/catalog/gb/search?term=${cleanUrlParameter(artist)}+${cleanUrlParameter(title)}&types=songs&limit=1"))
      .header(key = "Authorization", value = s"Bearer $appleClientBearerToken")
      .execute() map {
      case response if response.getStatusCode() == 200 =>
        response.as[AppleSearchResults].
          results.flatMap(_.
          songs.flatMap(_.
          data.flatMap(_.
          headOption.flatMap(_.
          attributes.flatMap(_.
          url
        )))))
      case response =>
        throw AppleClientException(s"Unexpected HTTP status code: ${response.getStatusCode()}", response.getStatusCode())
    }
  }
}
