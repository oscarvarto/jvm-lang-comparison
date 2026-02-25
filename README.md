# JVM Language Comparison: Validation with Error Accumulation

A comparison of how different JVM/native languages and approaches implement the same domain validation logic. Each
subproject validates a `Person` entity with three rules:

1. **Name** must not be blank
2. **Age** must not be negative
3. **Age** must not exceed `MAX_AGE` (130)

Most implementations share a core requirement: when multiple rules fail, **all** errors must be reported at once (error
accumulation), not just the first one found (fail-fast). The `simple-java` subproject deliberately uses fail-fast
validation to serve as a baseline showing the idiomatic Java approach without any FP machinery.

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

| Directory                                                                  | Language | FP Library         | Build System           | Error Strategy | Return type/value                                     |
|----------------------------------------------------------------------------|----------|--------------------|------------------------|----------------|-------------------------------------------------------|
| [`simple-java/`](simple-java/)                                             | Java 17  | —                  | Maven                  | Fail-fast      | `Person` or throws `IllegalArgumentException`         |
| [`java-no-lombok/`](java-no-lombok/)                                       | Java 25  | FunctionalJava     | Gradle                 | Accumulating   | `Validation<NonEmptyList<@ErrorMsg String>, Person>`  |
| [`traditional-is-not-always-simpler/`](traditional-is-not-always-simpler/) | Java 25  | FunctionalJava     | Gradle                 | Accumulating   | `Validation<NonEmptyList<@ErrorMsg String>, Person>`  |
| [`kotlin-validation/`](kotlin-validation/)                                 | Kotlin   | Arrow              | Gradle                 | Accumulating   | `Either<NonEmptyList<PersonValidationError>, Person>` |
| [`scala-validation/`](scala-validation/)                                   | Scala 3  | ZIO Prelude        | Mill                   | Accumulating   | `Validation[PersonValidationError, Person]`           |
| [`clojure-validation/`](clojure-validation/)                               | Clojure  | clojure.spec.alpha | deps.edn / tools.build | Accumulating   | `{:errors [...]}` or `{:ok {...}}`                    |
| [`jank-validation/`](jank-validation/)                                     | jank     | (plain predicates) | Leiningen + lein-jank  | Accumulating   | `{:errors [...]}` or `{:ok {...}}`                    |

## Running Tests

```bash
# Simple Java (Maven, fail-fast)
cd simple-java && mvn test

# Java without Lombok (Gradle, error accumulation)
cd java-no-lombok && ./gradlew test

# Java with Lombok + Checker Framework (Gradle, error accumulation)
cd traditional-is-not-always-simpler && ./gradlew test

# Kotlin
cd kotlin-validation && ./gradlew test

# Scala
cd scala-validation
mill app.test   # Testing ZIO version
mill cats.test  # Testing Cats version

# Clojure
cd clojure-validation && clj -T:build test

# jank
cd jank-validation && lein run
```
