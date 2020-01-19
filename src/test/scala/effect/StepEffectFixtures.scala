package effect

import cats.effect.{ContextShift, IO}
import fr.ramiro.play.actions.SuperStepFixtures
import fr.ramiro.play.cats.actions.StepIO
import org.scalactic.source
import org.scalatest.Suite
import play.api.mvc.Result

import scala.concurrent.Await
import scala.language.postfixOps

trait StepEffectFixtures extends SuperStepFixtures[IO] with StepIO {
  self: Suite =>
  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B =
    whenReady(step.value.unsafeToFuture())(block)(config, pos)

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A] =
    Await.result(step.value.unsafeToFuture(), config.timeout)
}
