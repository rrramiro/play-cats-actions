package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.data.Validated
import cats.syntax.either._
import play.api.data.Form
import play.api.libs.json.JsResult
import play.api.mvc.Result

import scala.util.{Either, Try}
import scala.language.{higherKinds, implicitConversions}

trait DefaultStepOps[F[_]] { self: SuperStep[F] =>

  implicit def eitherToStepOps[A, B](
      either: Either[B, A])(implicit mf: Applicative[F]): StepOps[A, B] = {
    (failureHandler: B => Result) =>
      Step(mf.pure(either.leftMap(failureHandler)))
  }

  implicit def validatedToStep[A, B](validated: Validated[B, A])(
      implicit mf: Applicative[F])
    : StepOps[A, B] = { (failureHandler: B => Result) =>
    Step(mf.pure(validated.leftMap(failureHandler).toEither))
  }

  implicit def optionToStepOps[A](option: Option[A])(
      implicit mf: Applicative[F]): StepOps[A, Unit] = {
    eitherToStepOps(Either.fromOption(option, ()))
  }

  implicit def jsResultToStepOps[A](jsResult: JsResult[A])(
      implicit mf: Applicative[F]): StepOps[A, JsErrorContent] = {
    eitherToStepOps(Either.fromJsResult(jsResult))
  }

  implicit def formToStepOps[A](form: Form[A])(
      implicit mf: Applicative[F]): StepOps[A, Form[A]] = {
    eitherToStepOps(Either.fromForm(form))
  }

  implicit def booleanToStepOps(boolean: Boolean)(
      implicit mf: Applicative[F]): StepOps[Unit, Unit] = {
    eitherToStepOps(Either.fromBoolean(boolean, ()))
  }

  implicit def tryToStepOps[A](tryValue: Try[A])(
      implicit mf: Applicative[F]): StepOps[A, Throwable] = {
    eitherToStepOps(Either.fromTry(tryValue))
  }

  implicit def fStepToResult(step: Step[Result])(
      implicit mf: Applicative[F]): F[Result] = {
    mf.map(step.value) { _.merge }
  }
}
