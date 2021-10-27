package top.top1.therealtuesdayweld.streams

import top.top1.therealtuesdayweld.config.Config

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor

trait Stream extends Config {
  implicit val system: ActorSystem

  implicit val executionContext: ExecutionContextExecutor

  def throttle[T](interval: FiniteDuration): Flow[T, T, NotUsed] =
    Flow[T].throttle(1, interval, 1, ThrottleMode.shaping)

  def throttleByOne[T](interval: FiniteDuration): Flow[T, T, NotUsed] =
    Flow[T].throttle(1, interval, 1, ThrottleMode.shaping)

  def oneAtATime[T]: Flow[Seq[T], T, NotUsed] =
    Flow[Seq[T]].mapConcat(_.toList)

}

