(ns oscarvarto.mx.domain.person-test
  (:require [clojure.test :refer [deftest is]]
            [oscarvarto.mx.domain.person :as person]))

(deftest name-cannot-be-blank-test
  (is (= {:errors [:blank-name]}
         (person/make-person "" 24))))

(deftest age-cannot-be-negative-test
  (is (= {:errors [:negative-age]}
         (person/make-person "Alice" -1))))

(deftest age-cannot-exceed-max-age-test
  (is (= {:errors [:max-age]}
         (person/make-person "Alice" 131))))

(deftest accumulates-multiple-errors-test
  (is (= {:errors [:blank-name :negative-age]}
         (person/make-person "" -1))))

(deftest valid-person-test
  (is (= {:ok {:name "Alice" :age 30}}
         (person/make-person "Alice" 30))))
