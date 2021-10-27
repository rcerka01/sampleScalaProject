package clients

import top.top1.therealtuesdayweld.client.{AppleClient, AppleClientException, AppleClientSettings} {FutureSupport, WiremockHelper}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.specs2.mock.Mockito
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.Scope

import scala.concurrent.duration._

class AppleClientSpec extends Specification with Mockito with WiremockHelper with FutureSupport {

  sequential

  "Apple Client client" should {
    "return Apple link if there is response from Apple Music API" in new AppleClientScope {

      val path = s"/v1/catalog/gb/songs"
      val resource = "/apple/link.json"
      getPathStub(path, resource)

      whenReady(appleClient.getUrlByIsrc("GBUM70708609")) { url =>
        url.get must be equalTo "https://itunes.apple.com/gb/album/call-the-shots/268694897?i=268694930"
      }
    }

    "return None if there is no response from Apple Music API" in new AppleClientScope {
      val path = s"/v1/catalog/gb/songs"
      val resource = "/apple/empty.json"
      getPathStub(path, resource)

      whenReady(appleClient.getUrlByIsrc("empty")) { url =>
        url must beNone
      }
    }

    "handle correct exceptions" in new AppleClientScope {
      val expectedStatusCode = 404

      waitUntilReady(appleClient.getUrlByIsrc("not-defined")) must
        throwAn(AppleClientException("Unexpected HTTP status code: 404", expectedStatusCode))
    }
  }

  trait AppleClientScope extends Scope with BeforeAfter {
    val host: String = "127.0.0.1"
    val port: Int = 6001

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

    val appleClient: AppleClient = new AppleClient(
      AppleClientSettings(
        httpClient = new HttpClient(),
        protocol = "http",
        host = host,
        port = port))
  }
}
