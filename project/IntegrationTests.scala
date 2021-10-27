import sbt.Keys._
import sbt._

object IntegrationTests {

  lazy val ItTest = config("it") extend Test

  lazy val settings = Defaults.itSettings ++ Seq(
    javaOptions in ItTest += "-Dconfig.file=src/main/resources/application.it.conf",
    fork in ItTest := true
  )
}
