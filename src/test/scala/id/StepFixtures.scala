package id

import akka.stream.Materializer
import fr.ramiro.play.cats.actions.id._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, MustMatchers}
import play.api.http.Status
import play.api.mvc.{Result, Results}
import scala.language.postfixOps

trait StepFixtures
    extends ScalaFutures
    with MustMatchers
    with Results
    with Status {

  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B) =
    block(step.value)

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
