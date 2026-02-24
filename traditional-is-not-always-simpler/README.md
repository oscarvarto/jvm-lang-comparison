# Java is not always simpler (Java)

Person validation using **FunctionalJava**, **Lombok**, and the **Checker Framework** on Java 25.

## Approach

Java lacks built-in sum types (sealed interfaces arrived in Java 17 but without exhaustiveness in expressions),
algebraic data types, and concise data class syntax. This project compensates with three external tools:

- **FunctionalJava** provides `Validation<E, A>`, `NonEmptyList<E>`, and `Semigroup` — the core applicative
  error-accumulation machinery.
- **Lombok** (`@Value`) eliminates the boilerplate of writing constructors, getters, `equals`, `hashCode`, and
  `toString` by hand.
- **Checker Framework** with a custom **Subtyping Checker** introduces a compile-time type alias `@ErrorMsg String` to
  distinguish error message strings from regular strings at the type level.

### Validation logic

A reusable `check` helper wraps FunctionalJava's `Validation.condition` and lifts the result into `NonEmptyList`:

```java
public static <T> Validation<NonEmptyList<@ErrorMsg String>, T> check(
        final boolean c, final @ErrorMsg String e, final T t) {
    return condition(c, e, t).nel();
}
```

The `Person.of` factory validates each field independently, then accumulates errors via FunctionalJava's
`Validation.accumulate` with a `NonEmptyList` semigroup:

```java
var validatedName   = check(!name.isBlank(), NAME_EMPTY_OR_WHITESPACE_ERROR_MSG, name);
var validatedMinAge = check(age >= 0,        NEGATIVE_AGE_ERROR_MSG,             age);
var validatedMaxAge = check(age <= MAX_AGE,  MAX_AGE_ERROR_MSG,                  age);
return validatedName.accumulate(
    nonEmptyListSemigroup(), validatedMinAge, validatedMaxAge,
    (n, a1, a2) -> new Person(n, a1));
```

### Type-alias via Checker Framework

The `@ErrorMsg` / `@NotErrorMsg` annotations form a two-level subtyping hierarchy enforced at compile time:

```text
@NotErrorMsg (top, default for unannotated strings)
    |
@ErrorMsg   (subtype — only string literals and explicitly annotated values)
```

This prevents accidentally passing an arbitrary `String` where an error message is expected, approximating a
newtype/type alias that Java does not natively support.

### Build complexity

The `build.gradle.kts` is non-trivial because it must:

1. Compile qualifier annotations (`@ErrorMsg`, `@NotErrorMsg`) **first** in a separate `compileQualifiers` task with
   `-proc:none`.
2. Feed those compiled classes back into the annotation processor classpath for both `compileJava` and
   `compileTestJava`.
3. Chain Lombok's annotation processor **before** the Checker Framework.
4. Work around Lombok's use of `sun.misc.Unsafe` on JDK 25 via forked javac with `--sun-misc-unsafe-memory-access=allow`.
5. Configure Spotless with Palantir Java Format for consistent formatting.

## Pros

- Java is the most widely adopted JVM language; the ecosystem and tooling are unmatched in breadth.
- FunctionalJava is a mature, well-tested library that provides a faithful implementation of the Validation applicative.
- The Checker Framework adds compile-time guarantees (nullness, custom type qualifiers) that go beyond what Java's type
  system offers out of the box.
- Lombok drastically reduces ceremony for data-carrying classes.

## Cons

- **Build complexity** — the Gradle build is the most involved of all four subprojects, with annotation processor
  ordering, multi-phase compilation, and JVM flag workarounds.
- **Library stacking** — three separate libraries (FunctionalJava, Lombok, Checker Framework) are needed to approximate
  what other languages provide natively or with a single library.
- **Error messages are strings** — even with `@ErrorMsg`, errors are still `String` values at runtime, not typed sealed
  members. Pattern matching on error kinds requires string comparison.
- **Verbose despite Lombok** — the overall ceremony (imports, static methods, annotation wiring) is still heavier than
  the Kotlin or Scala equivalents.

## Running

```bash
./gradlew test
```

## Key Dependencies

| Library             | Version         | Role                                        |
|---------------------|-----------------|---------------------------------------------|
| FunctionalJava      | 5.0             | `Validation`, `NonEmptyList`, `Semigroup`   |
| Lombok              | 1.18.42         | `@Value` for data classes                   |
| Checker Framework   | 3.53.1          | `@ErrorMsg` type alias via SubtypingChecker |
| Spotless + Palantir | 8.2.1 / 2.88.0  | Code formatting                             |
| TestNG + AssertJ    | 7.12.0 / 3.27.7 | Testing                                     |
