package prices.routes

import cats.effect.unsafe.implicits.global
import cats.effect._
import munit.FunSuite
import org.http4s._
import org.http4s.implicits._
import prices.data.InstanceKind
import prices.services.InstanceKindService
import prices.services.InstanceKindService.Exception.APICallFailure

class InstanceKindRoutesTest extends FunSuite {

  private def mockInstanceKindService[F[_]: Sync](response: F[List[InstanceKind]]): InstanceKindService[F] = () => response

  test("InstanceKindRoutes should return OK with a list of kinds on successful call") {
    val service = mockInstanceKindService[IO](IO.pure(List(InstanceKind("kind1"), InstanceKind("kind2"))))
    val routes  = InstanceKindRoutes[IO](service).routes.orNotFound

    val request = Request[IO](Method.GET, uri"/instance-kinds")
    val result  = routes.run(request).unsafeRunSync()

    assertEquals(result.status, Status.Ok)
  }

  test("InstanceKindRoutes should return FailedDependency on APICallFailure") {
    val errorMessage = "Smartcloud API call failed"
    val service      = mockInstanceKindService[IO](IO.raiseError(APICallFailure(errorMessage)))
    val routes       = InstanceKindRoutes[IO](service).routes.orNotFound

    val request = Request[IO](Method.GET, uri"/instance-kinds")
    val result  = routes.run(request).unsafeRunSync()

    assertEquals(result.status, Status.FailedDependency)
    val responseBody = result.as[String].unsafeRunSync()
    assert(responseBody.contains(errorMessage))
  }

  test("InstanceKindRoutes should return InternalServerError on generic errors") {
    val service = mockInstanceKindService[IO](IO.raiseError(new Exception("Generic failure")))
    val routes  = InstanceKindRoutes[IO](service).routes.orNotFound

    val request = Request[IO](Method.GET, uri"/instance-kinds")
    val result  = routes.run(request).unsafeRunSync()

    assertEquals(result.status, Status.InternalServerError)
    val responseBody = result.as[String].unsafeRunSync()
    assert(responseBody.contains("An unexpected error occurred"))
  }
}
