package oscarvarto.mx.domain

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

sealed interface PersonValidationError
data object BlankName : PersonValidationError
data object NegativeAge : PersonValidationError
data object MaxAge : PersonValidationError

@ConsistentCopyVisibility
data class Person private constructor(val name: String, val age: Int) {
    companion object {
        const val MAX_AGE = 130

        operator fun invoke(
            name: String,
            age: Int,
        ): Either<NonEmptyList<PersonValidationError>, Person> = either {
            zipOrAccumulate(
                { ensure(name.isNotBlank()) { BlankName } },
                { ensure(age >= 0) { NegativeAge } },
                { ensure(age <= MAX_AGE) { MaxAge } },
            ) { _, _, _ -> Person(name, age) }
        }
    }
}
