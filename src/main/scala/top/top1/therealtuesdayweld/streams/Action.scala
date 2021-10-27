package top.top1.therealtuesdayweld.streams

sealed trait Action
sealed trait Service

case object Apple extends Service
case object Spotify extends Service

case class UpdateAction(link: Option[String], service: Service) extends Action
case class DeleteAction(service: Service) extends Action
case object NoAction extends Action
