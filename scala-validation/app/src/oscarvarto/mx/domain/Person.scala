package oscarvarto.mx.domain

import zio.prelude.Validation
import PersonValidationError.*

enum PersonValidationError:
  case BlankName, NegativeAge, MaxAge

final case class Person private (name: String, age: Int)

object Person:
  val MAX_AGE = 130

  def make(
      name: String,
      age: Int
  ): Validation[PersonValidationError, Person] =
    Validation.validateWith(
      if name.isBlank then Validation.fail(BlankName)
      else Validation.succeed(name),
      if age < 0 then Validation.fail(NegativeAge)
      else Validation.succeed(age),
      if age > MAX_AGE then Validation.fail(MaxAge)
      else Validation.succeed(age)
    )((n, a, _) => new Person(n, a))
