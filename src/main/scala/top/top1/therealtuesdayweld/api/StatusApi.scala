package top.top1.therealtuesdayweld.api

import top.top1.therealtuesdayweld.domain.marshalling.JsonSerializers
import top.top1.therealtuesdayweld.domain.response.Status

trait StatusApi extends JsonSerializers {

  val statusRoutes = {
    path("status") {
      get {
        complete {
          Status("OK")
        }
      }
    }
  }

}
