package prices.routes

import cats.effect._
import cats.implicits._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{EntityEncoder, HttpRoutes}
import prices.routes.protocol._
import prices.services.InstanceKindService
import prices.services.InstanceKindService.Exception.APICallFailure

final case class InstanceKindRoutes[F[_]: Sync](instanceKindService: InstanceKindService[F]) extends Http4sDsl[F] {

  val prefix = "/instance-kinds"

  implicit val instanceKindResponseEncoder: EntityEncoder[F, List[InstanceKindResponse]] =
    jsonEncoderOf[F, List[InstanceKindResponse]]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instanceKindService.getAll().flatMap(kinds => Ok(kinds.map(k => InstanceKindResponse(k)))).recoverWith {
        case e: APICallFailure =>
          FailedDependency(s"Smartcloud API failed: ${e.message}")
        case _ =>
          InternalServerError("An unexpected error occurred")
      }
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
