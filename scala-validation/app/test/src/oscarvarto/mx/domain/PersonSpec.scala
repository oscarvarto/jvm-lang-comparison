package oscarvarto.mx.domain

import zio.NonEmptyChunk
import zio.test.*
import zio.prelude.Validation
import PersonValidationError.*

object PersonSpec extends ZIOSpecDefault:

  def spec = suite("PersonSpec")(
    test("name cannot be blank") {
      val result = Person.make("", 24)
      assertTrue(
        result.toEither == Left(NonEmptyChunk(BlankName))
      )
    },
    test("age cannot be negative") {
      val result = Person.make("Alice", -1)
      assertTrue(
        result.toEither == Left(NonEmptyChunk(NegativeAge))
      )
    },
    test("age cannot exceed MAX_AGE") {
      val result = Person.make("Alice", 131)
      assertTrue(
        result.toEither == Left(NonEmptyChunk(MaxAge))
      )
    },
    test("accumulates multiple errors") {
      val result = Person.make("", -1)
      assertTrue(
        result.toEither == Left(NonEmptyChunk(BlankName, NegativeAge))
      )
    },
    test("valid person") {
      val result = Person.make("Alice", 30)
      assertTrue(result.toEither.isRight)
    }
  )
