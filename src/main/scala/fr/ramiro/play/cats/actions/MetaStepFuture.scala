package fr.ramiro.play.cats.actions

import cats.Applicative
import cats.data.EitherT
import cats.syntax.either._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

trait MetaStepFuture
    extends MetaStep[Future]
    with DefaultLiftedStepOps[Future] {
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
