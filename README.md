# JVM Language Comparison: Validation with Error Accumulation

A comparison of how four JVM languages implement the same domain validation logic using functional programming patterns.
Each subproject validates a `Person` entity with three rules:

1. **Name** must not be blank
2. **Age** must not be negative
3. **Age** must not exceed `MAX_AGE` (130)

All implementations share the same core requirement: when multiple rules fail, **all** errors must be reported at once
(error accumulation), not just the first one found (fail-fast).

## The Validation Pattern

The central abstraction is an **Applicative Functor** — a structure that can combine independent computations while
accumulating their failures. Unlike monadic sequencing (`flatMap` / `>>=`), which short-circuits on the first error,
applicative combination runs every validation and merges failures via a **Semigroup** (an associative binary operation
for combining errors).

In pseudocode, the algorithm is:

```text
validate(name, age):
  v1 = if name.isBlank       then Failure(BlankName)   else Success(name)
  v2 = if age < 0            then Failure(NegativeAge)  else Success(age)
  v3 = if age > MAX_AGE      then Failure(MaxAge)       else Success(age)
  return combine(v1, v2, v3, (n, a, _) => Person(n, a))
           where combine accumulates all Failures into a NonEmpty collection
```

Key FP concepts at play:

| Concept                                | Role                                                                              |
|----------------------------------------|-----------------------------------------------------------------------------------|
| **Sum type** (sealed hierarchy)        | Represents the disjoint set of possible validation errors                         |
| **Product type** (data class / record) | The `Person` entity itself                                                        |
| **Validation / Either**                | A bifunctor holding either accumulated errors or a success value                  |
| **NonEmptyList / NonEmptyChunk**       | Guarantees at least one error exists in the failure case                          |
| **Semigroup**                          | Defines how to merge two error collections (concatenation)                        |
| **Applicative combination**            | `zipOrAccumulate` / `validateWith` / `accumulate` — runs all checks independently |

## Subprojects

| Directory                                                                  | Language | FP Library         | Build System           | Error Type                                            |
|----------------------------------------------------------------------------|----------|--------------------|------------------------|-------------------------------------------------------|
| [`traditional-is-not-always-simpler/`](traditional-is-not-always-simpler/) | Java 25  | FunctionalJava     | Gradle                 | `Validation<NonEmptyList<@ErrorMsg String>, Person>`  |
| [`kotlin-validation/`](kotlin-validation/)                                 | Kotlin   | Arrow              | Gradle                 | `Either<NonEmptyList<PersonValidationError>, Person>` |
| [`scala-validation/`](scala-validation/)                                   | Scala 3  | ZIO Prelude        | Mill                   | `Validation[PersonValidationError, Person]`           |
| [`clojure-validation/`](clojure-validation/)                               | Clojure  | clojure.spec.alpha | deps.edn / tools.build | `{:errors [...]}` or `{:ok {...}}`                    |

## Running Tests

```bash
# Java
cd traditional-is-not-always-simpler && ./gradlew test

# Kotlin
cd kotlin-validation && ./gradlew test

# Scala
cd scala-validation && mill app.test

# Clojure
cd clojure-validation && clj -T:build test
```
