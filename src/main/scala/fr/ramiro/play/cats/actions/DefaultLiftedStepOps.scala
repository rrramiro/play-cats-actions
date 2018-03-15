package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.data.{EitherT, Validated}
import cats.syntax.either._
import play.api.mvc.Result

import scala.util.Either
import scala.language.{higherKinds, implicitConversions}

trait DefaultLiftedStepOps[F[_]] extends DefaultStepOps[F] {
  self: MetaStep[F] =>
  implicit def fEitherToStepOps[A, B, E[_, _] <: Either[_, _]](
      fEither: F[E[B, A]])(implicit mf: Applicative[F]): StepOps[A, B] = {
    (failureHandler: B => Result) =>
      EitherT[F, Result, A](
        mf.map(fEither.asInstanceOf[F[Either[B, A]]])(
          _.leftMap(failureHandler)))
  }

  implicit def fValidatedToStepOps[A, B](fEither: F[Validated[B, A]])(
      implicit mf: Applicative[F])
    : StepOps[A, B] = { (failureHandler: B => Result) =>
    EitherT[F, Result, A](mf.map(fEither)(_.leftMap(failureHandler).toEither))
  }

  implicit def fBooleanToStepOps(future: F[Boolean])(
      implicit mf: Applicative[F]): StepOps[Unit, Unit] = {
    fEitherToStepOps(mf.map(future)(b => Either.fromBoolean(b, ())))
  }

  implicit def fOptionToStepOps[A, O[_] <: Option[_]](fOption: F[O[A]])(
      implicit mf: Applicative[F]): StepOps[A, Unit] = {
    fEitherToStepOps(mf.map(fOption) { opt =>
      Either.fromOption(opt.asInstanceOf[Option[A]], ())
    })
  }
}
