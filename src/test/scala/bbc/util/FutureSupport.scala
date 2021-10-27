package util

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait FutureSupport {

  protected val futureSupportTimeout: Duration = 5.second

  def waitUntilReady[Promised](future: Future[Promised]): Promised = {
    Await.result(future, futureSupportTimeout)
  }

  def whenReady[Promised, Return](future: Future[Promised])(block: Promised => Return): Return = {
    block(waitUntilReady(future))
  }
}
