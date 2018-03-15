package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.data.EitherT
import cats.syntax.either._
import play.api.mvc.Result

import scala.util.Either
import scala.language.{higherKinds, implicitConversions}

trait MetaStep[F[_]] {
  type Step[A] = EitherT[F, Result, A]

  trait StepOps[A, B] extends (StepHandler[B] => Step[A]) {

    def ?|(failureHandler: StepHandler[B]): Step[A] = apply(failureHandler)

    def ?|(failureThunk: => Result): Step[A] = apply(_ => failureThunk)
  }

  case object escalate

  object Step {
    def apply[A](step: F[Either[Result, A]]): Step[A] =
      EitherT[F, Result, A](step)

    def unit[A](a: A)(implicit mf: Applicative[F]): Step[A] =
      EitherT.rightT[F, Result](a)

    def fail[A](r: Result)(implicit mf: Applicative[F]): Step[A] =
      EitherT.leftT[F, A](r)
  }

  abstract class AbstractEscalateOps[A, B](future: F[A], mf: Applicative[F]) {
    def handle(function: StepHandler[B]): Step[A]
    def -|(escalateWord: escalate.type): EitherT[F, Result, A] =
      EitherT[F, Result, A](mf.map(future)(_.asRight[Result]))

    def -|(failureHandler: StepHandler[B]): Step[A] = handle(failureHandler)
    def -|(failureThunk: => Result): Step[A] = handle(_ => failureThunk)
  }

}
