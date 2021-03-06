package id

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.Validated
import org.scalatest.FunSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n._
import play.api.libs.json._
import play.api.mvc.Result
import scala.util.{Failure, Success, Try}
import cats.syntax.either._
import scala.language.higherKinds

class IdStepTest extends FunSuite with StepIdFixtures {
  implicit lazy val system: ActorSystem = ActorSystem()

  test("Escalate A to Step[A]") {
    whenStepReady(42 -| escalate) { _ must be(42.asRight[Result]) }
  }

  test("Promote Option[A] to Step[A]") {
    whenStepReady(Some(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady(None ?| NotFound) { _ must be(NotFound.asLeft[Int]) }
  }

  test("Promote Either[B, A] to Step[A]") {
    whenStepReady(42.asRight[String] ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady("foo".asLeft[Int] ?| (s => BadRequest(s))) {
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
    whenStepReady(true ?| NotFound) {
      _ must be(().asRight[Result])
    }
    whenStepReady(false ?| NotFound) {
      _ must be(NotFound.asLeft[Unit])
    }
  }

  test("Promote Try[A] to Step[A]") {
    whenStepReady(Success(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady((Failure(new Exception("foo")): Try[Int]) ?| (e =>
      BadRequest(e.getMessage))) {
      checkFailingResult[Int](BAD_REQUEST, "foo")
    }
  }

  test("Promote Step[Result] to Result") {
    val result1: Result = Step.unit(Ok("foo"))
    result1.header.status must be(OK)
    val result2: Result = Step.fail[Result](NotFound)
    result2.header.status must be(NOT_FOUND)
  }

  test("Promote Validated[B,A] to Step[A]") {
    whenStepReady(
      validatedToStep(Validated.Valid(42): Validated[String, Int]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(validatedToStep(
      Validated.Invalid("Error"): Validated[String, Int]) ?| NotFound) {
      _ must be(NotFound.asLeft[Int])
    }
  }

}
