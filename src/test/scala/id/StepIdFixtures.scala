package id

import cats.Id

import scala.language.postfixOps
import fr.ramiro.play.actions.SuperStepFixtures
import fr.ramiro.play.cats.actions.StepId
import org.scalactic.source
import org.scalatest.Suite
import play.api.mvc.Result

trait StepIdFixtures extends SuperStepFixtures[Id] with StepId { self: Suite =>
  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B = block(step.value)

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A] = step.value
}
