package fr.ramiro.play.cats

import cats.data.EitherT
import cats.syntax.either._
import cats.Applicative
import play.api.data.Form
import play.api.libs.json.{JsPath, JsResult, JsonValidationError}
import play.api.mvc.Result

import scala.language.{higherKinds, implicitConversions}
import scala.util.Either

package object actions {
  type JsErrorContent = Seq[(JsPath, Seq[JsonValidationError])]
  type StepHandler[B] = (B => Result)

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

  object future extends MetaStepFuture

  object id extends MetaStepId

  object effect extends MetaStepIO

  implicit class EitherObjectOpsPlay(val either: Either.type) {
    def fromForm[A](form: Form[A]): Either[Form[A], A] =
      form.fold(err => err.asLeft[A], _.asRight[Form[A]])
    def fromBoolean[A](boolean: Boolean, ok: A): Either[Unit, A] =
      if (boolean) ok.asRight[Unit] else ().asLeft[A]
    def fromJsResult[A](jsResult: JsResult[A]): Either[JsErrorContent, A] =
      jsResult.fold(_.asLeft[A], _.asRight[JsErrorContent])
  }
}
