package fr.ramiro.play.cats

import cats.Id
import cats.effect.IO
import cats.syntax.either._
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc.Result

import scala.concurrent.Future
import scala.language.{higherKinds, implicitConversions}
import scala.util.Either

package object actions {
  type JsErrorContent = Seq[(JsPath, Seq[JsonValidationError])]
  type StepHandler[B] = (B => Result)

  trait StepId extends SuperStep[Id] with DefaultStepOps[Id]

  trait StepFuture
      extends SuperStep[Future]
      with DefaultLiftedStepOps[Future]
      with StepFutureImplicits

  trait StepIO
      extends SuperStep[IO]
      with DefaultLiftedStepOps[IO]
      with StepEffectImplicits

  object id extends StepId

  object future extends StepFuture

  object effect extends StepIO

  implicit class EitherObjectOpsPlay(val either: Either.type) {
    def fromForm[A](form: Form[A]): Either[Form[A], A] =
      form.fold(err => err.asLeft[A], _.asRight[Form[A]])
    def fromBoolean[A](boolean: Boolean, ok: A): Either[Unit, A] =
      if (boolean) ok.asRight[Unit] else ().asLeft[A]
    def fromJsResult[A](jsResult: JsResult[A]): Either[JsErrorContent, A] =
      jsResult.fold(_.asLeft[A], _.asRight[JsErrorContent])
  }
}
