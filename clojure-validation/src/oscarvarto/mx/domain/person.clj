(ns oscarvarto.mx.domain.person
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(def max-age 130)

;; Individual validation specs
(s/def ::non-blank-name (s/and string? (complement str/blank?)))
(s/def ::non-negative-age #(>= % 0))
(s/def ::within-max-age #(<= % max-age))

(defn make-person
  "Creates a person map if all validations pass, or returns accumulated error keywords."
  [name age]
  (let [errors (cond-> []
                 (not (s/valid? ::non-blank-name name))  (conj :blank-name)
                 (not (s/valid? ::non-negative-age age)) (conj :negative-age)
                 (not (s/valid? ::within-max-age age))   (conj :max-age))]
    (if (seq errors)
      {:errors errors}
      {:ok {:name name :age age}})))
