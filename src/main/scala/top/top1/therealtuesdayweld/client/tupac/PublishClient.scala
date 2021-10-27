package top.top1.therealtuesdayweld.client.tupac

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import org.slf4s.Logging
import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.Future

class PublishClient(snsClient: AmazonSNS) extends Config with Logging {

  def publishMessage(message: String): Future[Int] = {
    val publishResult = snsClient.publish(new PublishRequest(snsTupacActions, message))
    log.debug(s"Tupac action SNS message ${publishResult.getMessageId} published.")
    Future.successful(1)
  }
}
