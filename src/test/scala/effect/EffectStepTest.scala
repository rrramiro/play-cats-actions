package effect

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.Validated
import cats.effect._
import cats.syntax.either._
import org.scalatest.FunSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class EffectStepTest extends FunSuite with StepEffectFixtures {
  implicit lazy val system: ActorSystem = ActorSystem()
  private lazy val executionContext: ExecutionContext =
    implicitly[Materializer].executionContext
  private implicit lazy val timer: Timer[IO] = IO.timer(executionContext)
  private implicit lazy val cs: ContextShift[IO] =
    IO.contextShift(executionContext)

  test("Promote IO[A] to Step[A]") {
    whenStepReady(IO.pure(42) ?| NotFound) { successful =>
      successful must be(42.asRight[Result])
    }
    whenStepReady(IO.raiseError[Int](new NullPointerException) ?| NotFound) {
      failure =>
        failure must be(NotFound.asLeft[Int])
    }
  }

  test("Escalate IO[A] to Step[A]") {
    whenStepReady(IO.pure(42) -| escalate) { _ must be(42.asRight[Result]) }
    an[NullPointerException] should be thrownBy {
      await(IO.raiseError[Int](new NullPointerException) -| escalate)
    }
  }

  test("Promote Option[A] to Step[A]") {
    whenStepReady(Some(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady(None ?| NotFound) { _ must be(NotFound.asLeft[Int]) }
  }

  test("Promote Either[B, A] to Step[A]") {
    whenStepReady(Right[String, Int](42) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(Left[String, Int]("foo") ?| (s => BadRequest(s))) {
      checkFailingResult[Int](BAD_REQUEST, "foo")
    }
  }

  test("Promote JsResult[A] to Step[A]") {
    val jsError = JsError("foo")
    whenStepReady(JsSuccess(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady(
      (jsError: JsResult[Int]) ?| (e => BadRequest(JsError.toJson(e)))) {
      checkFailingResult[Int](BAD_REQUEST,
                              Json.stringify(JsError.toJson(jsError.errors)))
    }
  }

  test("Promote Form[A] to Step[A]") {
    implicit val messages: Messages =
      MessagesImpl(Lang.defaultLang, new DefaultMessagesApi())
    val successfulForm =
      Form(single("int" -> number), Map("int" -> "42"), Nil, Some(42))
    val erroneousForm = successfulForm.withError("int", "foo")
    whenStepReady(successfulForm ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady(erroneousForm ?| (f => BadRequest(f.errorsAsJson))) {
      checkFailingResult[Int](BAD_REQUEST,
                              erroneousForm.errorsAsJson.toString())
    }
  }

  test("Promote Boolean to Step[A]") {
    whenStepReady(true ?| NotFound) { _ must be(().asRight[Result]) }
    whenStepReady(false ?| NotFound) { _ must be(NotFound.asLeft[Unit]) }
  }

  test("Promote Try[A] to Step[A]") {
    whenStepReady(Success(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady((Failure(new Exception("foo")): Try[Int]) ?| (e =>
      BadRequest(e.getMessage))) {
      checkFailingResult[Int](BAD_REQUEST, "foo")
    }
  }

  test("Promote Step[Result] to Result") {
    val result1: Result = stepToResult(Step.unit(Ok("foo")))
    result1.header.status must be(OK)
    val result2: Result = stepToResult(Step.fail[Result](NotFound))
    result2.header.status must be(NOT_FOUND)
  }

  test("Promote Step[Result] to Future[Result]") {
    whenReady(Step.unit(Ok("foo")): Future[Result]) { r =>
      r.header.status must be(OK)
    }
    whenReady(Step.fail[Result](NotFound): Future[Result]) { r =>
      r.header.status must be(NOT_FOUND)
    }
  }

  test("Promote promote FutureBoolean[Boolean] to Step[Unit]") {
    whenStepReady(IO.pure(true) ?| NotFound) { _ must be(().asRight[Result]) }
    whenStepReady(IO.pure(false) ?| NotFound) {
      _ must be(NotFound.asLeft[Unit])
    }
  }

  test("Promote IO[Option[A]] to Step[A]") {
    whenStepReady(IO.pure(Option(42)) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(IO.pure[Option[Int]](None) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }

  test("properly promote IO[Either[B, A]] to Step[A]") {
    whenStepReady(IO.pure(42.asRight[String]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(IO.pure("foo".asLeft[Int]) ?| ((s: String) => BadRequest(s))) {
      checkFailingResult[Int](BAD_REQUEST, "foo")
    }
  }

  test("Promote Validated[B,A] to Step[A]") {
    whenStepReady((Validated.Valid(42): Validated[String, Int]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(
      (Validated.Invalid("Error"): Validated[String, Int]) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }

  test("properly promote IO[Validated[B,A]] to Step[A]") {
    whenStepReady(
      IO.pure(Validated.Valid(42): Validated[String, Int]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(
      IO.pure(Validated.Invalid("Error"): Validated[String, Int]) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }

  test("properly promote Future[A] to Step[A]") {
    whenStepReady(Future.successful(42) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(Future.failed[Int](new NullPointerException) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }
}
