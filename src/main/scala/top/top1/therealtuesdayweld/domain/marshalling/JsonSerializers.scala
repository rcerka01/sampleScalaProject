package top.top1.therealtuesdayweld.domain.marshalling

import java.text.SimpleDateFormat

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.prefs.EmptyValueStrategy
import org.json4s.{DefaultFormats, jackson}

trait JsonSerializers extends Json4sSupport {

  private val defaultJsonFormat = new DefaultFormats {
    override val emptyValueStrategy = EmptyValueStrategy.preserve

    private val ISO8601 = "yyyy-MM-dd'T'HH:mm:ssZ"
    override def dateFormatter = new SimpleDateFormat(ISO8601)
  }

  implicit val serialization = jackson.Serialization
  implicit val formats = defaultJsonFormat ++ org.json4s.ext.JodaTimeSerializers.all
}
