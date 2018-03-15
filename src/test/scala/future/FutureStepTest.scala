package future

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.Validated
import cats.instances.future._
import cats.syntax.either._
import fr.ramiro.play.cats.actions.future._
import org.scalatest.FunSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.Status
import play.api.i18n._
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import play.api.mvc.{Result, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class FutureStepTest extends FunSuite with StepFixtures {

  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = ActorMaterializer()

  test("Promote Future[A] to Step[A]") {
    whenStepReady(Future.successful(42) -| NotFound) { successful =>
      successful must be(42.asRight[Result])
    }
    whenStepReady(Future.failed[Int](new NullPointerException) -| NotFound) {
      failure =>
        failure must be(NotFound.asLeft[Int])
    }
  }

  test("Escalate Future[A] to Step[A]") {
    whenStepReady(Future.successful(42) -| escalate) {
      _ must be(42.asRight[Result])
    }
    an[NullPointerException] should be thrownBy {
      await(Future.failed[Int](new NullPointerException) -| escalate)
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
    whenReady(Step.unit(Ok("foo")): Future[Result]) { r =>
      r.header.status must be(OK)
    }
    whenReady(Step.fail[Result](NotFound): Future[Result]) { r =>
      r.header.status must be(NOT_FOUND)
    }
  }

  test("Promote promote FutureBoolean[Boolean] to Step[Unit]") {
    whenStepReady(Future.successful(true) ?| NotFound) {
      _ must be(().asRight[Result])
    }
    whenStepReady(Future.successful(false) ?| NotFound) {
      _ must be(NotFound.asLeft[Unit])
    }
  }

  test("Promote Future[Option[A]] to Step[A]") {
    whenStepReady(Future.successful(Option(42)) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(Future.successful[Option[Int]](None) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }

  test("properly promote Future[Either[B, A]] to Step[A]") {
    whenStepReady(Future.successful(42.asRight[String]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(
      Future.successful("foo".asLeft[Int]) ?| ((s: String) => BadRequest(s))) {
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

  test("properly promote Future[Validated[B,A]] to Step[A]") {
    whenStepReady(
      Future
        .successful(Validated.Valid(42): Validated[String, Int]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(Future.successful(
      Validated.Invalid("Error"): Validated[String, Int]) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }
}
