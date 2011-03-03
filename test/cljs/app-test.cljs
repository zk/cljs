(ns app-test
  (:use app)
  (:import TestCase))


(defn is [test] ('assertTrue test))

(defn deftest [name & tests]
  (let [tc (TestCase name)]
    (map #(set! (aget tc.prototype (str "test-" (gensym-str)))
                %)
         tests)))

(deftest :foo
  #(is (= 1 2)))




