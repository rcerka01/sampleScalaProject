package clients

import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sns.AmazonSNS
import org.specs2.mock.Mockito
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.Scope
import top.top1.therealtuesdayweld.client.tupac.{PublishClient, TupacClient}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class TupacClientSpec  extends Specification with Mockito  with FutureSupport {

  sequential

  "Tupac client" should {
    "update Apple link in Tupac API" in new TupacClientScope {
      whenReady(tupacClient.updateAppleLink("nz346j", Some("apple_link"))) { result =>
        result must be equalTo 1
      }
    }

    "update Spotify link in Tupac API" in new TupacClientScope {
      whenReady(tupacClient.updateSpotifyLink("nz346j", Some("spotify_link"))) { result =>
        result must be equalTo 1
      }
    }

    "delete Apple link in Tupac API" in new TupacClientScope {
      whenReady(tupacClient.deleteMusicLink("nz346j", apple = true)) { result =>
        result must be equalTo 1
      }
    }

    "delete Spotify link in Tupac API" in new TupacClientScope {
      whenReady(tupacClient.deleteMusicLink("nz346j", spotify = true)) { result =>
        result must be equalTo 1
      }
    }

    "should not delete anything if no parameter is given Tupac API" in new TupacClientScope {
      tupacClient.deleteMusicLink("nz346j") must throwA[IllegalArgumentException](message = "link type is not given, please choose either apple or Spotify")
    }
  }

  trait TupacClientScope extends Scope with BeforeAfter {
    def before: Unit = {}

    def after: Unit = {}

    implicit val executionContext: ExecutionContextExecutor = ActorSystem("test").dispatcher
    implicit val completionTimeout: FiniteDuration = 1000.milliseconds

    val sns: AmazonSNS = mock[AmazonSNS]

    val publishResult: PublishResult = mock[PublishResult]
    publishResult.getMessageId returns "message id"

    def mockFunc(): AmazonSNS = sns

    lazy val snsClient: AmazonSNS = mockFunc()

    sns.publish(any) returns publishResult

    val publishClient = new PublishClient(snsClient)

    val testPublishClient: PublishClient = mock[PublishClient]

    val tupacClient: TupacClient = new TupacClient(publishClient)
  }
}
