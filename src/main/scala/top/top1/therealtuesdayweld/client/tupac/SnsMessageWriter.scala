package top.top1.therealtuesdayweld.client.tupac

import java.text.SimpleDateFormat
import java.util.Date

import org.slf4s.Logging

trait SnsMessageWriter extends Logging {

  def createUpdateSNSMessage(musicId: String, vendor: String, link: String): String = {
    val dateTime = new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date())
    val messageId = s"$dateTime $musicId UPDATE".stripMargin

    val updateMessage = s"""{\"music_id\": \"${musicId.trim}\", \"operation\": \"UPDATE\", \"provider\": \"${vendor.trim}\", \"link\": { \"value\": \"${link.trim}\"}}""".stripMargin

    createMessage(
      id = messageId,
      message = updateMessage)
  }

  def createDeleteSNSMessage(musicId: String, vendor: String): String = {
    val dateTime = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ss").format(new Date())

    val messageId = s"$dateTime-$musicId-DELETE"
    val deleteMessage = s"""{\"music_id\": \"${musicId.trim}\", \"operation\": \"DELETE\", \"provider\": \"${vendor.trim}\"}""".stripMargin

    createMessage(
      id = messageId,
      message = deleteMessage)
  }

  private def createMessage(id: String, message: String) = {
    log.debug(s"Message $id published.")
    message
  }
}
