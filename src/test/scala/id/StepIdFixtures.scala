package id

import cats.Id
import scala.language.postfixOps
import fr.ramiro.play.actions.MetaStepFixtures
import fr.ramiro.play.cats.actions.MetaStepId
import org.scalactic.source
import play.api.mvc.Result

trait StepIdFixtures extends MetaStepFixtures[Id] with MetaStepId {
  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B = block(step.value)

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A] = step.value
}
