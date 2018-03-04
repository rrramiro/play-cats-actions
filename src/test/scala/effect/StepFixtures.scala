package effect

import akka.stream.Materializer
import cats.effect._
import io.kanaka.monadic.dsl.effect._
import org.scalactic.source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, MustMatchers}
import play.api.mvc.Result
import scala.concurrent.Await
import scala.language.postfixOps

trait StepFixtures extends ScalaFutures with MustMatchers {

  def whenStepReady[T, U](step: Step[T])(fun: Either[Result, T] => U)(implicit config: PatienceConfig, pos: source.Position): U = {
    whenReady(step.value.unsafeToFuture())(fun)(config, pos)
  }

  def await[T](step: Step[T])(implicit config: PatienceConfig): Either[Result, T] = Await.result(step.value.unsafeToFuture(), config.timeout)

  def checkFailingResult[T](status: Int, message: String)(r: Either[Result, T])(implicit mat: Materializer): Assertion = r match {
    case Left(res) =>
      res.header.status must be(status)
      whenReady(res.body.consumeData){ value => value.decodeString("utf-8") must be(message) }
    case _ => fail("should be left")
  }
}
