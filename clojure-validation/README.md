# Clojure validation

Person validation using **clojure.spec.alpha** on Clojure 1.12.

## Approach

Clojure takes a fundamentally different path from the statically typed implementations. As a dynamic, functional Lisp
on the JVM, it replaces sealed hierarchies and generic types with **specs** (declarative predicates) and **plain data**
(maps, vectors, keywords).

### Validation logic

Individual rules are registered as specs:

```clojure
(s/def ::non-blank-name (s/and string? (complement str/blank?)))
(s/def ::non-negative-age #(>= % 0))
(s/def ::within-max-age #(<= % max-age))
```

The `make-person` function validates against each spec independently and accumulates error keywords into a vector using
`cond->`:

```clojure
(defn make-person [name age]
  (let [errors (cond-> []
                 (not (s/valid? ::non-blank-name name))  (conj :blank-name)
                 (not (s/valid? ::non-negative-age age)) (conj :negative-age)
                 (not (s/valid? ::within-max-age age))   (conj :max-age))]
    (if (seq errors)
      {:errors errors}
      {:ok {:name name :age age}})))
```

The return value is a plain map — `{:ok {:name "Alice" :age 30}}` on success, `{:errors [:blank-name :negative-age]}`
on failure. No wrapper types, no generics, no sealed hierarchies. Error "types" are keywords, which are interned,
self-describing, and namespace-qualified when needed.

### Line-by-line walkthrough (`src/oscarvarto/mx/domain/person.clj`)

The source file has 21 lines. This section explains what each line does in plain language.

Line 1 is `(ns oscarvarto.mx.domain.person`. It starts the namespace declaration. A namespace is similar to a module or
package name, so everything in this file belongs to `oscarvarto.mx.domain.person`.

Line 2 is `(:require [clojure.spec.alpha :as s]`. It begins the dependency list and imports Clojure Spec with the alias
`s`, which is why the file can call `s/def` and `s/valid?`.

Line 3 is `[clojure.string :as str]))`. It imports `clojure.string` as `str`. The closing `))` ends both the `:require`
form and the `ns` form started above.

Line 5 is `(def max-age 130)`. It defines a top-level var named `max-age` with value `130`, so the upper age limit is
centralized in one place.

Line 7 is `;; Individual validation specs`. This is a comment; `;;` means Clojure ignores it at runtime.

Line 8 is `(s/def ::non-blank-name (s/and string? (complement str/blank?)))`. It defines a spec called
`::non-blank-name`. The rule is: value must be a string and must not be blank. `complement` negates `str/blank?`, and
`::` creates a namespaced keyword tied to this namespace.

Line 9 is `(s/def ::non-negative-age #(>= % 0))`. It defines `::non-negative-age`. The `#(...)` syntax creates an
anonymous function; `%` is its argument. The check enforces age greater than or equal to zero.

Line 10 is `(s/def ::within-max-age #(<= % max-age))`. It defines `::within-max-age`, enforcing age less than or equal
to `max-age` (130).

Line 12 is `(defn make-person`. It starts defining the function named `make-person`.

Line 13 is the docstring: `"Creates a person map if all validations pass, or returns accumulated error keywords."` This
text documents the function and is visible in REPL help.

Line 14 is `[name age]`. It declares the function parameters: `name` and `age`.

Line 15 is `(let [errors (cond-> []`. It creates a local binding `errors`, starting from an empty vector `[]`, then uses
`cond->` to conditionally transform it.

Line 16 is `(not (s/valid? ::non-blank-name name)) (conj :blank-name)`. If `name` fails the non-blank-name spec,
`cond->` applies `(conj :blank-name)` and adds `:blank-name` to the error vector.

Line 17 is `(not (s/valid? ::non-negative-age age)) (conj :negative-age)`. If age is negative, it appends
`:negative-age`.

Line 18 is `(not (s/valid? ::within-max-age age)) (conj :max-age))]`. If age is above `max-age`, it appends `:max-age`.
The trailing `)]` closes the `cond->` expression and the `let` binding vector.

Line 19 is `(if (seq errors)`. It checks whether any errors were collected. `seq` returns a truthy value for non-empty
collections and `nil` for empty ones.

Line 20 is `{:errors errors}`. If errors exist, the function returns a map containing an `:errors` key and the vector of
error keywords.

Line 21 is `{:ok {:name name :age age}})))`. If there are no errors, the function returns success data under `:ok`,
including a nested map with `:name` and `:age`. The final `)))` closes `if`, `let`, and `defn`.

In short: `make-person` always returns plain data, either a success map (`:ok`) or an error map (`:errors`), never
exceptions for validation failures.

### Development workflow

Clojure's interactive nature shines with **Emacs + CIDER**. The `deps.edn` includes a `:cider-clj` alias for launching
an nREPL with CIDER middleware:

```bash
clj -M:cider-clj
```

Or use `cider-jack-in` directly from Emacs, which injects the middleware automatically. This enables a REPL-driven
workflow where you evaluate expressions incrementally, re-define functions on the fly, and inspect validation results
interactively — all without restarting the process.

## Pros

- **Simplicity** — no type hierarchies, no generics, no annotation processors. The entire validation is ~20 lines of
  data-oriented code.
- **Interactive development** — the REPL-driven workflow with CIDER enables rapid experimentation. You can test
  `make-person` with different inputs in real time without running a test suite.
- **Data as the universal interface** — inputs and outputs are plain maps and vectors. No serialization needed; values are
  directly printable, comparable, and composable.
- **clojure.spec** — specs serve double duty as validation rules **and** documentation. They can also generate test data
  via `test.check`.
- **Robust concurrency** — Clojure is a compiled language with persistent data structures, STM, and agents, providing an
  efficient infrastructure for multithreaded programming out of the box.

## Cons

- **No compile-time type safety** — errors are keywords, not typed values. A typo like `:blank-naem` won't be caught until
  runtime.
- **No enforced exhaustiveness** — unlike sealed hierarchies with pattern matching, there is no compiler check that all
  error cases are handled.
- **Spec is runtime-only** — `s/valid?` checks happen at runtime. There is no compile-time verification that the spec
  predicates are correct or complete.
- **Smaller JVM mindshare** — Clojure has a dedicated community but fewer developers and job postings than Java or Kotlin.

## Running

```bash
clj -T:build test
```

## Key Dependencies

| Library            | Version   | Role                                        |
| ------------------ | --------- | ------------------------------------------- |
| Clojure            | 1.12.4    | Language (includes clojure.spec.alpha)      |
| clojure.spec.alpha | (bundled) | Spec predicates for validation rules        |
| CIDER nREPL        | 0.58.0    | Emacs REPL middleware                       |
| test-runner        | 0.5.1     | Cognitect test runner for deps.edn projects |
