package oscarvarto.mx.domain

import cats.data.NonEmptyChain
import org.scalatest.funsuite.AnyFunSuite
import PersonValidationError.*

class PersonSpec extends AnyFunSuite:

  test("name cannot be blank") {
    val result = Person.make("", 24)
    assert(result.isInvalid)
    assert(result.toEither == Left(NonEmptyChain(BlankName)))
  }

  test("age cannot be negative") {
    val result = Person.make("Alice", -1)
    assert(result.isInvalid)
    assert(result.toEither == Left(NonEmptyChain(NegativeAge)))
  }

  test("age cannot exceed MAX_AGE") {
    val result = Person.make("Alice", 131)
    assert(result.isInvalid)
    assert(result.toEither == Left(NonEmptyChain(MaxAge)))
  }

  test("accumulates multiple errors") {
    val result = Person.make("", -1)
    assert(result.isInvalid)
    assert(result.toEither == Left(NonEmptyChain(BlankName, NegativeAge)))
  }

  test("valid person") {
    val result = Person.make("Alice", 30)
    assert(result.isValid)
  }
