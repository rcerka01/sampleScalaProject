package top.top1.therealtuesdayweld.client

import java.nio.charset.StandardCharsets
import com.github.nscala_time.time.Imports._
import top.top1.therealtuesdayweld.config.Config
import top.top1.therealtuesdayweld.domain.marshalling.JsonSerializers

import scala.concurrent.{ExecutionContext, Future}

  case class SpotifyResponse(tracks: Option[Tracks])

  case class Track(uri: Option[String])

  case class Tracks(
                     href: Option[String],
                     items: Option[List[Track]],
                     limit: Option[Int],
                     next: Option[String],
                     offset: Option[Int],
                     previous: Option[Int],
                     total: Option[Int]
                   )

case class SpotifyClientException(msg: String, status: Int) extends Exception(msg)

case class SpotifyClientSettings(httpClient: HttpClient, protocol: String, apiHost: String, accountsHost: String, port: Int)

case class SpotifyToken(
  access_token: Option[String],
  token_type: Option[String],
  expires_in: Option[Int],
  scope: Option[String],
  expires_at: Option[DateTime]
)

class SpotifyClient(settings: SpotifyClientSettings)
                   (implicit val executionContext: ExecutionContext) extends JsonSerializers with Config {

  private val Unauthorized = 401

  private def cleanUrlParameter(parameter: String) = parameter.replace('&', ' ')

  private def spotifyAccountUrl(path: String): String = UrlBuilder(
    protocol = settings.protocol,
    host = settings.accountsHost,
    port = settings.port,
    path = path
  )

  private def spotifyApiUrl(path: String): String = UrlBuilder(
    protocol = settings.protocol,
    host = settings.apiHost,
    port = settings.port,
    path = path
  )

  private lazy val encodeId = java.util.Base64.getEncoder.encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8))

  private var tokenStorage: Option[SpotifyToken] = None

  private def postAuthentication(credentials: String): Future[Option[SpotifyToken]] = {
    settings.httpClient
      .post(spotifyAccountUrl("/api/token"))
      .body("grant_type=client_credentials")
      .header(key = "Authorization", value = s"Basic $credentials")
      .header(key = "Content-Type", value = "application/x-www-form-urlencoded")
      .execute() map {
      case response if response.getStatusCode() == 200 => {
        val spotifyToken = response.as[Option[SpotifyToken]] map (_.copy(expires_at = Some(DateTime.now())))
        tokenStorage = spotifyToken
        spotifyToken
      }
      case response =>
        throw SpotifyClientException(s"Unexpected HTTP status code: ${response.getStatusCode()}", response.getStatusCode())
    }
  }

  private def getToken(tokenStorage: Option[SpotifyToken]): Future[Option[String]] = {
   tokenStorage match {
      case Some(token: SpotifyToken) =>  (token.expires_at map (expires => {
        if (expires < (DateTime.now() - 1.hour)) {
          postAuthentication(encodeId) map (_.flatMap(_.access_token))
        }
        else {
          Future.successful(token.access_token)
        }
      })).getOrElse(throw SpotifyClientException("Can't get Spotify Bearer token.", Unauthorized))
      case _ =>  postAuthentication(encodeId) map (_.flatMap (_.access_token))
    }
  }

  private def getUri(query: String): Future[Option[String]] = {
    getToken(tokenStorage) flatMap { bearerTokenOpt =>

      val bearerToken = bearerTokenOpt.getOrElse(throw SpotifyClientException("Can't get Spotify Bearer token", Unauthorized))

      settings.httpClient
        .get(spotifyApiUrl(s"/v1/search?$query&market=GB"))
        .header(key = "Authorization", value = s"Bearer $bearerToken")
        .execute() map {
        case response if response.getStatusCode() == 200 =>
          response.as[Option[SpotifyResponse]].flatMap(_.tracks.flatMap(_.items.flatMap(_.headOption.map(_.uri)))).flatten
        case response =>
          throw SpotifyClientException(s"Unexpected HTTP status code: ${response.getStatusCode()}", response.getStatusCode())
      }
    }
  }

  private def formatUrl(uri: String): String =
    s"https://open.spotify.com/track/${uri.split(":")(2)}"

  def getUriByIsrc(isrc: String): Future[Option[String]] = getUri(s"type=track&q=isrc:$isrc")

  def getUriByArtistAndTitle(artist: String, title: String): Future[Option[String]] =
    getUri(s"type=track&q=${cleanUrlParameter(title)}+${cleanUrlParameter(artist)}&limit=1")

  def getUrlByIsrc(isrc: String): Future[Option[String]] = getUriByIsrc(isrc) map { uriOpt =>
    uriOpt map ( uri => formatUrl(uri) )
  }

  def getUrlByArtistAndTitle(artist: String, title: String): Future[Option[String]] = getUriByArtistAndTitle(artist, title) map { uriOpt =>
    uriOpt map ( uri => formatUrl(uri) )
  }
}
