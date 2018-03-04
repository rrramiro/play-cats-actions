/*
 * Copyright 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package future

import cats.data.EitherT
import cats.instances.future._
import io.kanaka.monadic.dsl.future._
import org.scalacheck.Prop._
import org.scalacheck.{Prop, Properties}
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object StepSpecification extends Properties("Step") with StepFixtures {
  property("left identity") = Prop.forAll{ (int: Int , f: Int => Step[String] )  =>
    await(Step.unit[Int](int) flatMap f) == await(f(int))
  }

  property("right identity") = Prop.forAll{ ( step: Step[String] )  =>
    await(step flatMap Step.unit[String]) == await(step)
  }

  property("associativity") = Prop.forAll{ (step: EitherT[Future, Result,Int], f: Int => EitherT[Future, Result,String], g: String => EitherT[Future, Result,Boolean]) =>
    await((step flatMap f) flatMap g) == await(step flatMap(x => f(x) flatMap g))
  }
}
