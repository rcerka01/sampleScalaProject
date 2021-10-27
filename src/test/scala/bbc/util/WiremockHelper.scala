package util

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import scala.io.Source

trait WiremockHelper {

  private val OK = 200

  def readResourceFile(path: String): String = {
    Source.fromURL(getClass.getResource(path)).mkString
  }

  def getStub(path: String, resource: String, status: Int = OK, fixedDelay: Option[Integer] = None): StubMapping = {
    stubFor(get(urlEqualTo(path))
      .willReturn(
        aResponse()
          .withHeader("Content-type", "application")
          .withBody(readResourceFile(resource))
          .withStatus(status)
          .withFixedDelay(fixedDelay.getOrElse(0))
      )
    )
  }

  def getPathStub(path: String, resource: String, status: Int = OK, fixedDelay: Option[Integer] = None): StubMapping = {
    stubFor(get(urlPathEqualTo(path))
      .willReturn(
        aResponse()
          .withHeader("Content-type", "application")
          .withBody(readResourceFile(resource))
          .withStatus(status)
          .withFixedDelay(fixedDelay.getOrElse(0))
      )
    )
  }

  def postStub(path: String, resource: String, status: Int = OK, fixedDelay: Option[Integer] = None): StubMapping = {
    stubFor(post(urlPathEqualTo(path))
      .willReturn(
        aResponse()
          .withHeader("Content-type", "application")
          .withBody(readResourceFile(resource))
          .withStatus(status)
          .withFixedDelay(fixedDelay.getOrElse(0))
      )
    )
  }

  def deleteStub(path: String, status: Int = OK, fixedDelay: Option[Integer] = None): StubMapping = {
    stubFor(delete(urlPathEqualTo(path))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withFixedDelay(fixedDelay.getOrElse(0))
      )
    )
  }
}
