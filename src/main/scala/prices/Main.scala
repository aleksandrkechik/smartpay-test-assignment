package prices

import cats.effect.{ IO, IOApp }
import org.http4s.ember.client.EmberClientBuilder
import org.slf4j.{ Logger, LoggerFactory }
import prices.config.Config

object Main extends IOApp.Simple {

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  def run: IO[Unit]          = prepareServer()
  private def prepareServer(): IO[Unit] =
    for {
      config <- Config.load[IO]
      _      = logger.info("Config loaded")
      client = EmberClientBuilder.default[IO].build
      _      = logger.info("Ember-client created")
      _      = logger.info("Starting server...")
      _ <- Server.serve(config, client).compile.drain
    } yield ()
}
