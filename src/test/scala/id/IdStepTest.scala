package id

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.Validated
import org.scalatest.FunSuite
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import play.api.mvc.Result
import scala.util.{Failure, Success, Try}
import cats.syntax.either._
import io.kanaka.monadic.dsl.id._
import scala.language.higherKinds

class IdStepTest extends FunSuite with StepFixtures {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = ActorMaterializer()
  //TODO
  test("Promote Future[A] to Step[A]") {
    whenStepReady(42 -| NotFound) { successful =>
      successful must be(42.asRight[Result])
    }
//    whenStepReady(({throw new NullPointerException}: Int) -| NotFound) { failure =>
//      failure must be(NotFound.asLeft[Int])
//    }
  }

  //TODO
  test("Escalate Future[A] to Step[A]") {
    whenStepReady(42 -| escalate) { _ must be(42.asRight[Result]) }
//    an[NullPointerException] should be thrownBy {
//      ({ throw new NullPointerException }:Int) -| escalate
//    }
  }

  test("Promote Option[A] to Step[A]") {
    whenStepReady(Some(42) ?| NotFound) { _ must be(42.asRight[Result]) }
    whenStepReady(None ?| NotFound) { _ must be(NotFound.asLeft[Int]) }
  }
  //TODO
  test("Promote Either[B, A] to Step[A]") {
    //fEitherToStepOps
    //eitherToStepOps

    //implicit def t[A, B, E[_,_] <: Either[A,B]](either: E[A, B]): dsl.id.StepOps[B, A] = eitherToStepOps(either.asInstanceOf[Either[A,B]])

    whenStepReady(eitherToStepOps(42.asRight[String]) ?| NotFound) {
      _ must be(42.asRight[Result])
    }
    whenStepReady(eitherToStepOps("foo".asLeft[Int]) ?| (s => BadRequest(s))) {
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
    //booleanToStepOps
    //fBooleanToStepOps
    whenStepReady(booleanToStepOps(true) ?| NotFound) {
      _ must be(().asRight[Result])
    }
    whenStepReady(booleanToStepOps(false) ?| NotFound) {
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
    //validatedToStep
    //fValidatedToStepOps
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
