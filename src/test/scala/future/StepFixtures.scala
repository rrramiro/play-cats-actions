package future

import akka.stream.Materializer
import fr.ramiro.play.cats.actions.future.Step
import org.scalactic.source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, MustMatchers}
import play.api.http.Status
import play.api.mvc.{Result, Results}

import scala.concurrent.Await

trait StepFixtures
    extends ScalaFutures
    with MustMatchers
    with Results
    with Status {

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
