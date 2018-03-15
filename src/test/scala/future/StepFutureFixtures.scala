package future

import fr.ramiro.play.actions.MetaStepFixtures
import fr.ramiro.play.cats.actions.MetaStepFuture
import org.scalactic.source
import play.api.mvc.Result

import scala.concurrent.{Await, Future}

trait StepFutureFixtures extends MetaStepFixtures[Future] with MetaStepFuture {
  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B = whenReady(step.value)(block)(config, pos)

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A] =
    Await.result(step.value, config.timeout)
}
