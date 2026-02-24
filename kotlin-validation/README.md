# Kotlin validation

Person validation using **Arrow** on Kotlin.

## Approach

Kotlin's language features — `sealed interface`, `data class`, `data object`, and operator overloading — make it
straightforward to express the validation pattern with minimal ceremony. Arrow provides the FP primitives (`Either`,
`NonEmptyList`, `zipOrAccumulate`) that integrate naturally with Kotlin's type system and coroutine-based DSL (Raise).

### Validation logic

The entire implementation fits in a single file. Error types are a sealed interface with data objects, and the factory
function uses Arrow's `Raise` DSL:

```kotlin
operator fun invoke(name: String, age: Int):
    Either<NonEmptyList<PersonValidationError>, Person> = either {
    zipOrAccumulate(
        { ensure(name.isNotBlank()) { BlankName } },
        { ensure(age >= 0) { NegativeAge } },
        { ensure(age <= MAX_AGE) { MaxAge } },
    ) { _, _, _ -> Person(name, age) }
}
```

`zipOrAccumulate` runs each lambda independently inside a `Raise<NonEmptyList<E>>` context. If any `ensure` fails, it
short-circuits that individual branch. Once all branches complete, failures are accumulated into a `NonEmptyList`, or
the success combiner produces a `Person`.

The `operator fun invoke` lets callers write `Person("Alice", 30)` as if calling a constructor, but the return type is
`Either` — making the validation boundary explicit at the type level.

### Testing

Kotest with the `kotest-assertions-arrow` module provides idiomatic matchers:

```kotlin
Person("", 24).shouldBeLeft(nonEmptyListOf(BlankName))
Person("Alice", 30).shouldBeRight()
```

## Pros

- **Concise** — sealed interfaces, data objects, and Arrow's DSL keep the implementation to ~30 lines with no boilerplate.
- **Type-safe errors** — validation errors are a sealed hierarchy, enabling exhaustive `when` expressions. No strings
  involved.
- **Excellent IDE support** — IntelliJ IDEA (from JetBrains, Kotlin's creator) provides first-class navigation,
  refactoring, and Arrow-aware inspections.
- **Modern language** — null safety, extension functions, coroutines, and context parameters are built-in.
- **Android ecosystem** — Kotlin is the de facto language for Android development, making this style directly transferable
  to mobile projects.

## Cons

- **Arrow is required** — Kotlin's standard library has no `Either` or `Validation` type; Arrow is effectively mandatory
  for this style of FP.
- **DSL learning curve** — the `Raise` context and `zipOrAccumulate` pattern are powerful but require familiarity with
  Arrow's effect system.
- **Sealed hierarchy verbosity** — compared to Scala's `case object` or Clojure's keywords, defining `data object
  BlankName : PersonValidationError` for each error is slightly more ceremonious.

## Running

```bash
./gradlew test
```

## Key Dependencies

| Library    | Version | Role                                                 |
|------------|---------|------------------------------------------------------|
| Arrow Core | 2.2.1.1 | `Either`, `NonEmptyList`, `Raise`, `zipOrAccumulate` |
| Kotest     | 6.1.3   | Test runner + Arrow assertion matchers               |
| Kotlin     | 2.3.10  | Language (JVM toolchain targeting JDK 25)            |
