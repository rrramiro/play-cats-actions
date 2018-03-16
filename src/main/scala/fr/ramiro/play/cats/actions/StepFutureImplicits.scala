package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.syntax.either._
import play.api.mvc.Result
import scala.concurrent.{ExecutionContext, Future}

trait StepFutureImplicits { self: SuperStep[Future] =>
  implicit def futureToStepOps[A](future: Future[A])(
      implicit ec: ExecutionContext,
      mf: Applicative[Future]): StepOps[A, Throwable] = {
    (failureHandler: Throwable => Result) =>
      Step(mf.map(future)(_.asRight[Result]).recover {
        case t: Throwable => failureHandler(t).asLeft[A]
      })
  }
}
