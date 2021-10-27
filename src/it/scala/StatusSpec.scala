import org.scalatest.{FlatSpec, Matchers}
import top.top1.therealtuesdayweld.api.StatusApi
import top.top1.therealtuesdayweld.domain.response.Status

class StatusSpec extends FlatSpec
  with Matchers
  with ScalatestRouteTest
  with StatusApi {

  "The service" should "respond with a 200 at /status" in {
    Get("/status") ~> statusRoutes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Status] shouldBe Status("OK")
    }
  }

}
