package fr.ramiro.play.actions

import akka.stream.Materializer
import fr.ramiro.play.cats.actions.MetaStep
import org.scalactic.source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, MustMatchers}
import play.api.http.Status
import play.api.mvc.{Result, Results}

import scala.language.{higherKinds, implicitConversions}

trait MetaStepFixtures[F[_]]
    extends ScalaFutures
    with MustMatchers
    with Results
    with Status { self: MetaStep[F] =>

  def whenStepReady[A, B](step: Step[A])(block: Either[Result, A] => B)(
      implicit config: PatienceConfig,
      pos: source.Position): B

  def await[A](step: Step[A])(
      implicit config: PatienceConfig): Either[Result, A]

  def checkFailingResult[A](status: Int, message: String)(r: Either[Result, A])(
      implicit mat: Materializer): Assertion = r match {
    case Left(res) =>
      res.header.status must be(status)
      whenReady(res.body.consumeData) { value =>
        value.decodeString("utf-8") must be(message)
      }
    case _ => fail("should be left")
  }
}
