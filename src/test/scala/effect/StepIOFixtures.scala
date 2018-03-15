package effect

import cats.effect.IO
import fr.ramiro.play.actions.MetaStepFixtures
import fr.ramiro.play.cats.actions.MetaStepIO
import org.scalactic.source
import play.api.mvc.Result

import scala.concurrent.Await
import scala.language.postfixOps

trait StepIOFixtures extends MetaStepFixtures[IO] with MetaStepIO {
  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B =
    whenReady(step.value.unsafeToFuture())(block)(config, pos)

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A] =
    Await.result(step.value.unsafeToFuture(), config.timeout)
}
