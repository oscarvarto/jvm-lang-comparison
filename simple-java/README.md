# Simple Java

Person validation using **plain Java 17** with Maven. No Lombok, no Checker Framework, no FunctionalJava.

## Approach

This is the baseline: the simplest possible Java implementation of the same business rules. Validation uses fail-fast
`if` checks in the constructor, throwing `IllegalArgumentException` on the first violation. There are no custom
types, no annotation processors, no functional programming libraries, and no Gradle — just a Maven POM with two test
dependencies.

### Validation logic

The constructor validates inline. Each rule is a plain `if` statement:

```java
public Person(String name, int age) {
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
        throw new IllegalArgumentException("Name cannot be empty or contain only white space");
    }
    if (age < 0) {
        throw new IllegalArgumentException("Age cannot be negative");
    }
    if (age > MAX_AGE) {
        throw new IllegalArgumentException("Age cannot be bigger than " + MAX_AGE + " years");
    }
    this.name = name;
    this.age = age;
}
```

This is the standard Java idiom: preconditions in the constructor, exceptions for violations. Any Java developer can
read this without knowing any library or framework.

### What is different from the other Java subprojects

| Aspect              | `traditional-is-not-always-simpler` | `java-no-lombok`         | `simple-java`         |
|---------------------|-------------------------------------|--------------------------|-----------------------|
| Java version        | 25                                  | 25                       | 17 (LTS)              |
| Build system        | Gradle (Kotlin DSL)                 | Gradle (Kotlin DSL)      | Maven                 |
| Build file size     | 146 lines                           | 105 lines                | 57 lines              |
| Error handling      | Applicative accumulation            | Applicative accumulation | Fail-fast exceptions  |
| Errors reported     | All at once                         | All at once              | First one only        |
| FunctionalJava      | Yes                                 | Yes                      | No                    |
| Checker Framework   | Yes                                 | Yes                      | No                    |
| Lombok              | Yes                                 | No                       | No                    |
| Source files        | 5                                   | 5                        | 1                     |
| `Person.java` size  | 32 lines                            | 62 lines                 | 52 lines              |
| Config files needed | `lombok.config`, version catalog    | Version catalog          | None (just `pom.xml`) |

## Pros

- **Minimal complexity** — one source file, one test file, a 57-line `pom.xml`. No annotation processors, no multi-phase
  compilation, no version catalogs, no plugin wiring.
- **Zero learning curve** — `if`/`throw` is the first pattern any Java developer learns. No library APIs to memorize, no
  DSLs to understand.
- **Any IDE, any build** — works with any Java 17+ JDK and any Maven installation. No Gradle wrapper, no plugin
  compatibility concerns.
- **Fast builds** — no annotation processing overhead. `mvn clean verify` completes in ~3 seconds.
- **LTS baseline** — Java 17 is a long-term support release with broad industry adoption. No dependency on preview
  features or bleeding-edge JDK versions.

## Cons

- **Fail-fast only** — the first violated rule throws immediately. If a form has three invalid fields, the user sees one
  error at a time. The other subprojects report all errors at once.
- **No type-level safety** — error messages are plain `String`s. Nothing prevents passing an arbitrary string where an
  error message is expected. The Checker Framework versions catch this at compile time.
- **No null safety** — `null` checks are manual (`Objects.requireNonNull`). Forgetting one is a silent bug until runtime.
  The Checker Framework's NullnessChecker catches this at compile time.
- **Boilerplate** — `equals`, `hashCode`, `toString`, and getters must be written by hand (or IDE-generated) for every
  data class. Java has no `data class`, `case class`, or `@Value` equivalent before records (Java 16+), and records have
  their own limitations.
- **Exception-based control flow** — callers must use try/catch to handle validation failures. There is no type-safe
  return value that forces the caller to handle the error case. A `Person` reference either exists or an exception was
  thrown — the type system does not distinguish the two paths.

## Running

```bash
mvn test
```

## Key Dependencies

| Library          | Version | Role           |
|------------------|---------|----------------|
| TestNG           | 7.12.0  | Test framework |
| AssertJ          | 3.27.7  | Test assertions|
| Java             | 17      | Language (LTS) |
| Maven            | 3.9+    | Build system   |
