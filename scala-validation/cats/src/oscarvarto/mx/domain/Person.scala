package oscarvarto.mx.domain

import cats.data.ValidatedNec
import cats.syntax.all.*
import PersonValidationError.*

enum PersonValidationError:
  case BlankName, NegativeAge, MaxAge

final case class Person private (name: String, age: Int)

object Person:
  val MAX_AGE = 130

  def make(
      name: String,
      age: Int
  ): ValidatedNec[PersonValidationError, Person] =
    val validName: ValidatedNec[PersonValidationError, String] =
      if name.isBlank then BlankName.invalidNec
      else name.validNec
    val validAge: ValidatedNec[PersonValidationError, Int] =
      if age < 0 then NegativeAge.invalidNec
      else age.validNec
    val notTooOld: ValidatedNec[PersonValidationError, Int] =
      if age > MAX_AGE then MaxAge.invalidNec
      else age.validNec

    (validName, validAge, notTooOld).mapN((n, a, _) => new Person(n, a))
