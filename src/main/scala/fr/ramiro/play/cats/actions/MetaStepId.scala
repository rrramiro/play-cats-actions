package fr.ramiro.play.cats.actions

import cats.{Applicative, Id}
import cats.data.EitherT
import cats.syntax.either._
import play.api.mvc.Result

trait MetaStepId extends MetaStep[Id] with DefaultStepOps[Id] {
  implicit class EscalateOps[A](future: Id[A])(implicit mf: Applicative[Id])
      extends AbstractEscalateOps[A, Throwable](future, mf) {
    def handle(failureHandler: StepHandler[Throwable]): Step[A] = {
      EitherT[Id, Result, A](mf.map(future)(_.asRight[Result]))
    }
  }
}
