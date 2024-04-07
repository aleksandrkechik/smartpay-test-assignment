package prices.services

import cats.implicits._
import cats.effect._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.{ Header, _ }
import org.slf4j.{ Logger, LoggerFactory }
import org.typelevel.ci.CIStringSyntax
import prices.data._
import prices.services.InstanceKindService.Exception.APICallFailure

object SmartcloudInstanceKindService {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Concurrent](
      config: Config,
      clientResource: Resource[F, Client[F]]
  ): InstanceKindService[F] =
    new SmartcloudInstanceKindService(config, clientResource)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      config: Config,
      clientResource: Resource[F, Client[F]]
  ) extends InstanceKindService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    private val getAllUri = s"${config.baseUri}/instances"

    private def sendRequest: F[List[InstanceKind]] = {
      def addAuthHeader(client: Client[F]): Client[F] = Client[F] { req =>
        client
          .run(
            req.withHeaders(Header.Raw(ci"Authorization", s"Bearer ${config.token}"))
          )
      }

      clientResource.use { c =>
        addAuthHeader(c)
          .expect[List[String]](getAllUri)(instanceKindsEntityDecoder)
          .map(_.map(InstanceKind.apply))
          .handleErrorWith { error =>
            logger.error("API call failed", error)
            Concurrent[F].raiseError(APICallFailure(error.getMessage))
          }
      }
    }

    override def getAll(): F[List[InstanceKind]] =
      sendRequest

  }

}
