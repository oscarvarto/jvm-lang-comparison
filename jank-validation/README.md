# Jank validation

Person validation using **Jank**, a Clojure dialect that compiles natively via LLVM. **NOTE**: Jank is pretty new, and
in heavy development.

## Approach

Jank is a general-purpose programming language that brings Clojure's semantics to native compilation through C++ codegen
and LLVM JIT. Since Jank is a Clojure dialect, the validation logic is nearly identical to the Clojure implementation
but with one key difference: `clojure.spec.alpha` is not available in Jank's alpha release, so validation predicates are
defined as plain functions instead of specs.

### Validation logic

Individual rules are plain predicate functions:

```clojure
(defn non-blank-name? [s]
  (and (string? s) (not (str/blank? s))))

(defn non-negative-age? [n]
  (>= n 0))

(defn within-max-age? [n]
  (<= n max-age))
```

The `make-person` function validates against each predicate independently and accumulates error keywords into a vector
using `cond->` — the same pattern as the Clojure version:

```clojure
(defn make-person [name age]
  (let [errors (cond-> []
                 (not (non-blank-name? name))  (conj :blank-name)
                 (not (non-negative-age? age)) (conj :negative-age)
                 (not (within-max-age? age))   (conj :max-age))]
    (if (seq errors)
      {:errors errors}
      {:ok {:name name :age age}})))
```

### Differences from the Clojure version

| Aspect           | Clojure                                          | Jank                                  |
| ---------------- | ------------------------------------------------ | ------------------------------------- |
| Validation rules | `clojure.spec.alpha` specs (`s/def`, `s/valid?`) | Plain predicate functions             |
| File extension   | `.clj`                                           | `.jank`                               |
| Runtime          | JVM (HotSpot)                                    | Native (LLVM JIT / AOT)               |
| Build system     | deps.edn + tools.build                           | Leiningen + lein-jank                 |
| Test runner      | `clj -T:build test` (Cognitect test-runner)      | `lein run` (programmatic `run-tests`) |

The validation logic itself — `cond->` accumulation, keyword errors, map return values — is identical. The only
structural difference is replacing spec predicates with regular functions, which arguably makes the code simpler.

### Testing

Jank ships its own `clojure.test` implementation with `deftest`, `is`, and `run-tests`. Since `lein test` is not yet
wired up Jank's alpha, the main entry point runs the tests programmatically:

```clojure
(defn -main [& args]
  (run-tests 'oscarvarto.mx.domain.person-test))
```

## Pros

- **Native compilation** — Jank compiles to native code via LLVM, producing executables that start instantly without JVM
  warmup.
- **Clojure compatibility** — the same mental model, data structures, and idioms transfer directly from Clojure.
- **C++ interop** — Jank can call into C++ libraries directly, opening up system-level programming that Clojure on the JVM
  cannot easily reach.
- **Simpler validation** — without spec, the predicate functions are straightforward and have no framework dependency.

## Cons

- **Alpha stage** — Jank 0.1-alpha probably will crash, leak, and be slow. Large areas of functionality are not yet
  implemented.
- **No `clojure.spec`** — spec-based validation, generative testing (`test.check`), and spec-as-documentation are
  unavailable.
- **No protocols or records** — not yet implemented, limiting extensibility patterns.
- **Limited tooling** — no CIDER/nREPL integration, no Emacs middleware, no IDE plugins yet. Development is primarily
  terminal-based.
- **`lein test` not wired up** — tests must be invoked programmatically via a main entry point.

## Running

```bash
lein run
```

## Key Dependencies

| Component      | Version    | Role                               |
| -------------- | ---------- | ---------------------------------- |
| Jank           | 0.1-alpha  | Language and compiler              |
| lein-jank      | 0.5        | Leiningen plugin for Jank projects |
| clojure.string | (built-in) | `blank?` for name validation       |
| clojure.test   | (built-in) | `deftest`, `is`, `run-tests`       |
