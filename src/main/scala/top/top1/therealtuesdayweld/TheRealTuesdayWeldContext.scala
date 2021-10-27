package top.top1.therealtuesdayweld

import client.http.HttpClient
import client.monitoring.Monitoring
import com.amazonaws.regions.Regions
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import org.slf4s.Logging
import top.top1.therealtuesdayweld.client.tupac.{PublishClient, TupacClient}
import top.top1.therealtuesdayweld.client.{AppleClient, AppleClientSettings, SegmentsDbClient, SpotifyClient, SpotifyClientSettings}
import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object TheRealTuesdayWeldContext extends Config with Logging with Monitoring {

  implicit val system =ActorSystem(serviceName)

  private val httpClient: HttpClient = new HttpClient(timeout = 5.seconds)

  private val database = Database.forConfig("database")

  val segmentsDbClient: SegmentsDbClient = new SegmentsDbClient(
    database = database
  )(system.dispatcher)

  lazy val snsClient: AmazonSNS = AmazonSNSClientBuilder.standard.withRegion(Regions.EU_WEST_1).build
  val publishClient = new PublishClient(snsClient)

  val tupacClient: TupacClient = new TupacClient(publishClient)

  val spotifyClient: SpotifyClient = new SpotifyClient(
    SpotifyClientSettings(
      httpClient = httpClient,
      protocol = spotifyClientProtocol,
      accountsHost = spotifyClientAccountsHost,
      apiHost = spotifyClientApiHost,
      port = spotifyClientPort))

  val appleClient: AppleClient = new AppleClient(
    AppleClientSettings(
      httpClient = httpClient,
      protocol = appleClientProtocol,
      host = appleClientHost,
      port = appleClientPort))

}
