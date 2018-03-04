package id

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.Id
import cats.data.EitherT
import io.kanaka.monadic.dsl.id.Step
import org.scalatest.{Assertion, MustMatchers}
import play.api.http.Status
import play.api.mvc.{Result, Results}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait StepFixtures extends MustMatchers  with Results with Status{
  implicit val timeout: Duration = 30 seconds

  def whenStepReady[A, B](step: EitherT[Id, Result, A])(block: Either[Result, A] => B ) = block(step.value)
  def whenReady[B](result: Result)(block: Result => B) = block(result)

  def checkFailingResult[T](status: Int, message: String)(r: Either[Result, T])(implicit mat: Materializer): Assertion = r match {
    case Left(res) =>
      res.header.status must be(status)
      val v = Await.result(res.body.consumeData, timeout)
      v.decodeString("utf-8") must be(message)
    case _ => fail("should be left")
  }
}
