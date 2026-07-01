(ns kotoba.lang.katex.parse-test
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.lang.katex.parse :as parse]))

(deftest numbers-and-symbols
  (is (= [:row [[:num "123"]]] (parse/parse "123")))
  (is (= [:row [[:num "3.14"]]] (parse/parse "3.14")))
  (is (= [:row [[:sym "x"]]] (parse/parse "x")))
  ;; digit run followed by a '.' that is NOT followed by another digit stops
  ;; before the '.' (so "3." parses as the number "3" then an :op ".")
  (is (= [:row [[:num "3"] [:op "."]]] (parse/parse "3."))))

(deftest implicit-multiplication-is-just-adjacency
  (is (= [:row [[:num "2"] [:sym "x"]]] (parse/parse "2x")))
  (is (= [:row [[:sym "x"] [:sym "y"] [:sym "z"]]] (parse/parse "xyz"))))

(deftest greek-letters
  (is (= [:row [[:sym "α"]]] (parse/parse "\\alpha")))
  (is (= [:row [[:sym "β"] [:sym "γ"]]] (parse/parse "\\beta \\gamma")))
  (is (= [:row [[:sym "Δ"]]] (parse/parse "\\Delta"))))

(deftest common-operators
  (is (= [:row [[:sym "a"] [:op "≤"] [:sym "b"]]] (parse/parse "a \\leq b")))
  (is (= [:row [[:op "∑"]]] (parse/parse "\\sum")))
  (is (= [:row [[:op "×"] [:op "±"] [:op "∞"]]]
         (parse/parse "\\times \\pm \\infty"))))

(deftest ordinary-operator-characters-pass-through
  (is (= [:row [[:sym "a"] [:op "+"] [:sym "b"] [:op "="] [:sym "c"]]]
         (parse/parse "a+b=c")))
  (is (= [:row [[:op "("] [:sym "x"] [:op ")"]]] (parse/parse "(x)"))))

(deftest unknown-command-fallback
  (is (= [:row [[:unknown "foo"]]] (parse/parse "\\foo")))
  (is (= [:row [[:sym "a"] [:unknown "bar"] [:sym "b"]]]
         (parse/parse "a\\bar b"))))

(deftest grouping
  (is (= [:row [[:group [[:sym "x"] [:op "+"] [:sym "y"]]]]]
         (parse/parse "{x+y}")))
  ;; a single-child group is unwrapped, not double-wrapped
  (is (= [:row [[:sym "x"]]] (parse/parse "{x}"))))

(deftest fractions
  (is (= [:row [[:frac [:num "1"] [:num "2"]]]] (parse/parse "\\frac{1}{2}")))
  ;; nested fractions
  (is (= [:row [[:frac [:frac [:num "1"] [:num "2"]] [:num "3"]]]]
         (parse/parse "\\frac{\\frac{1}{2}}{3}")))
  ;; brace-less single-token shorthand
  (is (= [:row [[:frac [:num "1"] [:num "2"]]]] (parse/parse "\\frac12"))))

(deftest superscript-and-subscript
  (is (= [:row [[:sup [:sym "x"] [:num "2"]]]] (parse/parse "x^2")))
  (is (= [:row [[:sub [:sym "x"] [:sym "i"]]]] (parse/parse "x_i")))
  ;; both orders of ^ and _ collapse into the same :supsub shape
  (is (= [:row [[:supsub [:sym "x"] [:sym "i"] [:num "2"]]]]
         (parse/parse "x^2_i")))
  (is (= [:row [[:supsub [:sym "x"] [:sym "i"] [:num "2"]]]]
         (parse/parse "x_i^2"))))

(deftest sqrt-and-nth-root
  (is (= [:row [[:sqrt nil [:group [[:sym "x"] [:op "+"] [:num "1"]]]]]]
         (parse/parse "\\sqrt{x+1}")))
  (is (= [:row [[:sqrt [:num "3"] [:sym "x"]]]] (parse/parse "\\sqrt[3]{x}")))
  (is (= [:row [[:sqrt nil [:sym "x"]]]] (parse/parse "\\sqrt x"))))

(deftest sum-with-subscript-and-superscript
  (is (= [:row [[:supsub [:op "∑"]
                 [:group [[:sym "i"] [:op "="] [:num "1"]]]
                 [:sym "n"]]]]
         (parse/parse "\\sum_{i=1}^{n}"))))

(deftest whitespace-is-insignificant-between-tokens
  (is (= (parse/parse "x + y") (parse/parse "x+y")))
  (is (= (parse/parse "\\frac{1}{2}") (parse/parse "\\frac { 1 } { 2 }"))))

(deftest malformed-input-throws
  (is (thrown? #?(:clj Exception :cljs js/Error) (parse/parse "\\frac{1}{2"))))
