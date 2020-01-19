package fr.ramiro.play.actions

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import fr.ramiro.play.cats.actions.{JsErrorContent, SuperStep}
import org.scalactic.source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, BeforeAndAfterAll, MustMatchers, Suite}
import play.api.data.Form
import play.api.http.Status
import play.api.libs.json.JsResult
import play.api.mvc.{Result, Results}
import cats.syntax.either._
import scala.language.{higherKinds, implicitConversions}
import scala.util.Either

trait SuperStepFixtures[F[_]]
    extends ScalaFutures
    with MustMatchers
    with Results
    with Status
    with BeforeAndAfterAll { self: SuperStep[F] with Suite =>

  def system: ActorSystem

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

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

  implicit class EitherObjectOpsPlay(val either: Either.type) {
    def fromForm[A](form: Form[A]): Either[Form[A], A] =
      form.fold(err => err.asLeft[A], _.asRight[Form[A]])
    def fromBoolean[A](boolean: Boolean, ok: A): Either[Unit, A] =
      if (boolean) ok.asRight[Unit] else ().asLeft[A]
    def fromJsResult[A](jsResult: JsResult[A]): Either[JsErrorContent, A] =
      jsResult.fold(_.asLeft[A], _.asRight[JsErrorContent])
  }
}
