package clients

import top.top1.therealtuesdayweld.client.{SpotifyClient, SpotifyClientException, SpotifyClientSettings} .http.HttpClient
import therealtuesdayweld.client._
import {FutureSupport, WiremockHelper}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.specs2.mock.Mockito
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.Scope
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SpotifyClientSpec  extends Specification with Mockito with WiremockHelper with FutureSupport {

  sequential

  "Spotify Client client" should {
    "return Spotify link via ISRC search" in new SpotifyClientScope {
      val tokenPath = "/api/token"
      val tokenResource = "/spotify/token.json"
      postStub(tokenPath, tokenResource)

      val path = "/v1/search?type=track&q=isrc:BE6F51100830&market=GB"
      val resource = "/spotify/link.json"
      getStub(path, resource)

      whenReady(spotifyClient.getUrlByIsrc("BE6F51100830")) { url =>
        url.get must be equalTo "https://open.spotify.com/track/1CfHy4E69qZifycQOk0eVH"
      }
    }

    "return Spotify link via search by artist and title" in new SpotifyClientScope {
      val tokenPath = "/api/token"
      val tokenResource = "/spotify/token.json"
      postStub(tokenPath, tokenResource)

      val path = "/v1/search?type=track&q=title+artist&limit=1&market=GB"
      val resource = "/spotify/link.json"
      getStub(path, resource)

      whenReady(spotifyClient.getUrlByArtistAndTitle("artist", "title")) { url =>
        url.get must be equalTo "https://open.spotify.com/track/1CfHy4E69qZifycQOk0eVH"
      }
    }

    "return None if there is no response from Spotify API" in new SpotifyClientScope {
      val tokenPath = "/api/token"
      val tokenResource = "/spotify/token.json"
      postStub(tokenPath, tokenResource)

      val path = "/v1/search?type=track&q=isrc:empty&market=GB"
      val resource = "/spotify/empty.json"
      getStub(path, resource)

      whenReady(spotifyClient.getUrlByIsrc("empty")) { url =>
        url must beNone
      }
    }

    "return Bad Request if wrong authorization sent to Spotify" in new SpotifyClientScope {
      val BadRequest = 400

      val tokenPath = "/api/token"
      val badTokenResource = "/spotify/bad.json"
      postStub(tokenPath, badTokenResource, BadRequest)

      val path = "/v1/search?type=track&q=isrc:BE6F51100830&market=GB"
      val resource = "/spotify/link.json"
      getStub(path, resource)

      waitUntilReady(spotifyClient.getUrlByIsrc("BE6F51100830")) must
        throwAn(SpotifyClientException("Unexpected HTTP status code: 400", BadRequest))
    }

    "return Unauthorized if wrong authorization token is sent to Spotify" in new SpotifyClientScope {
      val Unauthorized = 401

      val tokenPath = "/api/token"
      val tokenResource = "/spotify/unauthorized.json"
      postStub(tokenPath, tokenResource)

      val path = "/v1/search?type=track&q=isrc:unauthorized&market=GB"
      val resource = "/spotify/unauthorized.json"
      getStub(path, resource, Unauthorized)

      waitUntilReady(spotifyClient.getUrlByIsrc("unauthorized")) must
        throwAn(SpotifyClientException("Can't get Spotify Bearer token", Unauthorized))
    }

    "handle correct exceptions" in new SpotifyClientScope {
      val expectedStatusCode = 404

      waitUntilReady(spotifyClient.getUrlByIsrc("not-defined")) must
        throwAn(SpotifyClientException("Unexpected HTTP status code: 404", expectedStatusCode))
    }
  }

  trait SpotifyClientScope extends Scope with BeforeAfter {
    val host: String = "127.0.0.1"
    val port: Int = 6002

    private val wireMockServer = new WireMockServer(
      wireMockConfig()
        .port(port)
    )

    def before: Unit = {
      wireMockServer.start()
      WireMock.configureFor(host, port)
    }

    def after: Unit = {
      wireMockServer.stop()
    }

    implicit val completionTimeout = 1000.milliseconds

    val spotifyClient: SpotifyClient = new SpotifyClient(
      SpotifyClientSettings(
        httpClient = new HttpClient(),
        protocol = "http",
        apiHost = host,
        accountsHost = host,
        port = port))
  }
}
