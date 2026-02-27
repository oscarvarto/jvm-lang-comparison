# Scala validation

Person validation using **ZIO Prelude** and **Cats** on Scala 3.

This project contains two parallel implementations of the same validation logic, one using the ZIO ecosystem and one
using the Typelevel ecosystem, to compare both Scala FP approaches side by side.

## ZIO Prelude (`app` module)

Scala 3's type system — sealed traits, case objects, case classes with private constructors, and significant indentation
— expresses the validation pattern naturally. ZIO Prelude provides `Validation[+E, +A]`, a dedicated applicative type
that accumulates errors into `NonEmptyChunk[E]`.

### Validation logic

Error types are a sealed trait hierarchy. The `Person.make` factory combines three independent validations with
`Validation.validateWith`:

```scala
enum PersonValidationError:
  case BlankName, NegativeAge, MaxAge

final case class Person private (name: String, age: Int)

object Person:
  val MAX_AGE = 130

  def make(name: String, age: Int): Validation[PersonValidationError, Person] =
    Validation.validateWith(
      if name.isBlank then Validation.fail(BlankName)
      else Validation.succeed(name),
      if age < 0 then Validation.fail(NegativeAge)
      else Validation.succeed(age),
      if age > MAX_AGE then Validation.fail(MaxAge)
      else Validation.succeed(age)
    )((n, a, _) => new Person(n, a))
```

`Validation.validateWith` evaluates all three sub-validations and, on failure, concatenates their errors into
a `NonEmptyChunk`. On success, the combiner function produces a `Person`. Unlike `Either` (which is monadic and
fail-fast), `Validation` is designed specifically for applicative accumulation.

## Cats (`cats` module)

The Typelevel alternative uses `cats.data.ValidatedNec[E, A]` — `Validated` backed by `NonEmptyChain` — for the same
applicative error accumulation.

### Validation logic

The same sealed trait error hierarchy is used. The `Person.make` factory uses `.validNec` / `.invalidNec` extension
syntax and `(v1, v2, v3).mapN` for applicative combination:

```scala
import cats.data.ValidatedNec
import cats.syntax.all.*

def make(name: String, age: Int): ValidatedNec[PersonValidationError, Person] =
  val validName =
    if name.isBlank then BlankName.invalidNec
    else name.validNec
  val validAge =
    if age < 0 then NegativeAge.invalidNec
    else age.validNec
  val notTooOld =
    if age > MAX_AGE then MaxAge.invalidNec
    else age.validNec

  (validName, validAge, notTooOld).mapN((n, a, _) => new Person(n, a))
```

The `.mapN` call uses Cats' `Apply` instance for `Validated` to combine all validations applicatively and accumulate
errors via `NonEmptyChain`'s `Semigroup`.

## ZIO Prelude vs Cats comparison

| Aspect                  | ZIO Prelude                               | Cats                                      |
| ----------------------- | ----------------------------------------- | ----------------------------------------- |
| Error accumulation type | `Validation[E, A]` with `NonEmptyChunk`   | `ValidatedNec[E, A]` with `NonEmptyChain` |
| Combining validations   | `Validation.validateWith(v1, v2, v3)(f)`  | `(v1, v2, v3).mapN(f)`                    |
| Creating successes      | `Validation.succeed(x)`                   | `x.validNec`                              |
| Creating failures       | `Validation.fail(e)`                      | `e.invalidNec`                            |
| Test framework          | ZIO Test (`ZIOSpecDefault`, `assertTrue`) | ScalaTest (`AnyFunSuite`, `assert`)       |
| Ecosystem               | ZIO (effects, streams, layers)            | Typelevel (cats-effect, fs2, http4s)      |

Both approaches produce equivalent results. ZIO Prelude's `validateWith` is more explicit, while Cats' `.mapN` relies on
the tuple syntax extension and typeclass derivation.

## Build system

This project uses **Mill** instead of sbt. A shared `SharedModule` trait extracts common configuration:

```scala
trait SharedModule extends ScalaModule, ScalafmtModule {
  def scalaVersion = "3.8.1"
}

object app extends SharedModule { /* ZIO deps */ }
object cats extends SharedModule { /* Cats deps */ }
```

The `private` constructor on the case class prevents direct instantiation, forcing all callers through the `make`
factory.

## Pros

- **Powerful type system** — sealed traits with case objects/enums provide exhaustive pattern matching. Generic types,
  higher-kinded types, and variance annotations (`+E`, `+A`) are first-class citizens.
- **Minimal boilerplate** — Scala 3's significant indentation, `case object`, and `case class` keep the implementation
  very compact.  
- **Two ecosystems** — both ZIO and Typelevel are well-supported, giving developers a choice based on their preferences
  and project needs.
- **Mill build** — faster startup, simpler configuration, and Scala-native build definitions compared to sbt's complex
  DSL.

## Cons

- **Smaller ecosystem** — fewer libraries, fewer developers, and less corporate adoption compared to Java or Kotlin.  
- **Compilation speed** — Scala 3 compilation is slower than Kotlin or Java, though Mill's caching helps.
- **IDE support** — IntelliJ's Scala plugin and Metals (for VS Code) are capable but not as polished as IntelliJ's
  native Kotlin support.

## Running

```bash
mill app.test     # ZIO Prelude tests
mill cats.test    # Cats tests
```

## Key Dependencies

| Library     | Version    | Module | Role                                            |
| ----------- | ---------- | ------ | ----------------------------------------------- |
| ZIO Prelude | 1.0.0-RC46 | `app`  | `Validation`, `NonEmptyChunk`                   |
| ZIO Test    | 2.1.21     | `app`  | Test framework (`ZIOSpecDefault`, `assertTrue`) |
| Cats Core   | 2.13.0     | `cats` | `ValidatedNec`, `mapN`, applicative syntax      |
| ScalaTest   | 3.2.19     | `cats` | Test framework (`AnyFunSuite`, `assert`)        |
| Scala       | 3.8.1      | both   | Language                                        |
| Mill        | 1.1.2      | both   | Build system                                    |
