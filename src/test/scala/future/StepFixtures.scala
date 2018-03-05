package future

import akka.stream.Materializer
import cats.Applicative
import cats.data.EitherT
import fr.ramiro.play.cats.actions.future.Step
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, MustMatchers}
import play.api.mvc.{Result, Results}

import scala.concurrent.{Await, Future}

trait StepFixtures extends ScalaFutures with MustMatchers {

  implicit def arbitraryResult: Arbitrary[Result] = Arbitrary(
    Gen.oneOf(Results.NotFound,
              Results.NoContent,
              Results.Ok,
              Results.InternalServerError,
              Results.BadGateway)
  )

  implicit def arbitraryStepA[A](
      implicit arbA: Arbitrary[A],
      arbResult: Arbitrary[Result],
      ap: Applicative[Future]): Arbitrary[EitherT[Future, Result, A]] = {
    Arbitrary(
      for {
        isLeft <- arbitrary[Boolean]
        a <- arbitrary[A](arbA)
        result <- arbitrary[Result](arbResult)
      } yield {
        if (isLeft) {
          EitherT.leftT[Future, A](result)
        } else {
          EitherT.rightT[Future, Result](a)
        }
      }
    )
  }

  def whenStepReady[T, U](step: Step[T])(fun: Either[Result, T] => U)(
      implicit config: PatienceConfig,
      pos: source.Position): U = {
    whenReady[Either[Result, T], U](step.value)(fun)(config, pos)
  }

  def await[T](step: Step[T])(
      implicit config: PatienceConfig): Either[Result, T] =
    Await.result(step.value, config.timeout)

  def checkFailingResult[T](status: Int, message: String)(r: Either[Result, T])(
      implicit mat: Materializer): Assertion = r match {
    case Left(res) =>
      res.header.status must be(status)
      whenReady(res.body.consumeData) { value =>
        value.decodeString("utf-8") must be(message)
      }
    case _ => fail("should be left")
  }
}
