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
|--------------------|-----------|---------------------------------------------|
| Clojure            | 1.12.4    | Language (includes clojure.spec.alpha)      |
| clojure.spec.alpha | (bundled) | Spec predicates for validation rules        |
| CIDER nREPL        | 0.58.0    | Emacs REPL middleware                       |
| test-runner        | 0.5.1     | Cognitect test runner for deps.edn projects |
