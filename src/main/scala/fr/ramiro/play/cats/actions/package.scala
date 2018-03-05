package fr.ramiro.play.cats

import cats.data.{EitherT, Validated}
import cats.effect.IO
import cats.syntax.either._
import cats.{Applicative, Id}
import play.api.data.Form
import play.api.libs.json.{JsPath, JsResult, JsonValidationError}
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.{Either, Try}

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

    implicit def fEitherToStepOps[A, B, E[_,_] <: Either[_,_]](fEither: F[E[B, A]])(
        implicit mf: Applicative[F])
      : StepOps[A, B] = { (failureHandler: B => Result) =>
      EitherT[F, Result, A](mf.map(fEither.asInstanceOf[F[Either[B,A]]])(_.leftMap(failureHandler)))
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

    //TODO
    implicit def fOptionToStepOps[A, O[_] <: Option[_]](fOption: F[O[A]])(
        implicit mf: Applicative[F]): StepOps[A, Unit] = {
      fEitherToStepOps(mf.map(fOption) { opt =>
        Either.fromOption(opt.asInstanceOf[Option[A]], ())
      })
    }

    implicit def eitherToStepOps[A, B](
        either: Either[B, A])(implicit mf: Applicative[F]): StepOps[A, B] = {
      (failureHandler: B => Result) =>
        EitherT[F, Result, A](mf.pure(either.leftMap(failureHandler)))
    }

    implicit def validatedToStep[A, B](validated: Validated[B, A])(
        implicit mf: Applicative[F])
      : StepOps[A, B] = { (failureHandler: B => Result) =>
      EitherT[F, Result, A](mf.pure(validated.leftMap(failureHandler).toEither))
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

    implicit def fStepToResult[A](step: EitherT[F, A, A])(
        implicit mf: Applicative[F]): F[A] = {
      mf.map(step.value) { _.merge }
    }
  }

  object future extends MetaStep[Future] {
    implicit class EscalateOps[A](future: Future[A])(
        implicit ec: ExecutionContext,
        mf: Applicative[Future])
        extends AbstractEscalateOps[A, Throwable](future, mf) {
      def handle(failureHandler: StepHandler[Throwable]): Step[A] = {
        EitherT[Future, Result, A](
          mf.map(future)(_.asRight[Result]).recover {
            case t: Throwable => failureHandler(t).asLeft[A]
          }
        )
      }
    }
  }

  object id extends MetaStep[Id] {
    implicit class EscalateOps[A](future: Id[A])(implicit mf: Applicative[Id])
        extends AbstractEscalateOps[A, Throwable](future, mf) {
      def handle(failureHandler: StepHandler[Throwable]): Step[A] = {
        EitherT[Id, Result, A](mf.map(future: Id[A])(_.asRight[Result]))
      }
    }
  }

  object effect extends MetaStep[IO] {
    implicit class EscalateOps[A](effect: IO[A])(implicit mf: Applicative[IO])
        extends AbstractEscalateOps[A, Throwable](effect, mf) {
      def handle(failureHandler: StepHandler[Throwable]): Step[A] = {
        EitherT[IO, Result, A](mf.map(effect.attempt) {
          case Right(a) => a.asRight[Result]
          case Left(t)  => failureHandler(t).asLeft[A]
        })
      }
    }

    implicit def stepToResult(step: EitherT[IO, Result, Result])(
        implicit mf: Applicative[IO]): Result = {
      mf.map(step.value) { _.merge }.unsafeRunSync()
    }

    implicit def stepToFutureResult(step: EitherT[IO, Result, Result])(
        implicit mf: Applicative[IO]): Future[Result] = {
      mf.map(step.value) { _.merge }.unsafeToFuture()
    }

    implicit def fEitherToStepOps[A](future: Future[A])(
        implicit ec: ExecutionContext,
        mf: Applicative[IO]): StepOps[A, Throwable] = {
      (failureHandler: Throwable => Result) =>
        EitherT[IO, Result, A](mf.map(IO.fromFuture(IO.pure(future)).attempt) {
          case Right(a) => a.asRight[Result]
          case Left(t)  => failureHandler(t).asLeft[A]
        })
    }

  }

  implicit class EitherObjectOpsPlay(val either: Either.type) {
    def fromForm[A](form: Form[A]): Either[Form[A], A] =
      form.fold(err => err.asLeft[A], _.asRight[Form[A]])
    def fromBoolean[A](boolean: Boolean, ok: A): Either[Unit, A] =
      if (boolean) ok.asRight[Unit] else ().asLeft[A]
    def fromJsResult[A](jsResult: JsResult[A]): Either[JsErrorContent, A] =
      jsResult.fold(_.asLeft[A], _.asRight[JsErrorContent])
  }
}
