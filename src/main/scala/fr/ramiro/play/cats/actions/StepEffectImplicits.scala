package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.effect.{ContextShift, IO}
import cats.syntax.either._
import play.api.mvc.Result
import scala.concurrent.Future

trait StepEffectImplicits { self: SuperStep[IO] =>

  implicit def effectToStepOps[A](effect: IO[A])(
      implicit mf: Applicative[IO]): StepOps[A, Throwable] = {
    (failureHandler: Throwable => Result) =>
      Step(mf.map(effect.attempt) {
        _.leftMap(failureHandler)
      })
  }

  implicit def stepToResult(step: Step[Result])(
      implicit mf: Applicative[IO]): Result = {
    mf.map(step.value) { _.merge }.unsafeRunSync()
  }

  implicit def stepToFutureResult(step: Step[Result])(
      implicit mf: Applicative[IO]): Future[Result] = {
    mf.map(step.value) { _.merge }.unsafeToFuture()
  }

  implicit def fEitherToStepOps[A](future: Future[A])(
      implicit cs: ContextShift[IO],
      mf: Applicative[IO]): StepOps[A, Throwable] = {
    effectToStepOps(IO.fromFuture(mf.pure(future)))
  }

}
