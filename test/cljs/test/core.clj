(ns cljs.test.core
  (:use [cljs.core] :reload)
  (:use [clojure.test])
  (:require [cljs.rhino :as rhino])
  (:import (org.mozilla.javascript Context
                                   Scriptable
                                   NativeArray
                                   NativeObject)))

;; ## Test helpers

(defn narr-to-seq [narr]
  (->> narr
    (.getIds)
    (seq)
    (map #(.get narr % nil))))

(defn obj-to-map [obj]
  (let [obj-ids (seq (.getIds obj))
        vals (map #(.get obj % nil) obj-ids)
        keys (map keyword obj-ids)]
    (apply hash-map (interleave keys vals))))

(defn eval-js [& stmt-or-stmts]
  (let [cmd (str
             *core-lib*
             "\n\n"
             (apply wrap-with-ns "test" []
                    stmt-or-stmts))
        res (rhino/eval-js cmd)]
    (cond
     (= NativeArray (class res)) (narr-to-seq res)
     (= NativeObject (class res)) (obj-to-map res)
     :else res)))

(deftest test-core
  (is (rhino/eval-js *core-lib*)))

(deftest test-var-definition
  (is (= "hello"
         (eval-js '(def x "hello")
                  'x))))

(deftest test-function-definition
  (is (= "hello"
         (eval-js '(defn x [] "hello")
                  '(x)))))

(deftest test-hash-definition
  (is (= "hello"
         (eval-js '(#(str "he" "llo"))))))

(deftest test-varargs
  (is (= [2 3 4])
      (eval-js '(defn x [& args]
                  (map (fn [i] (+ i 1)) args))
                    '(x 1 2 3))))

(deftest test-varargs-2
  (is (= 10
         (eval-js '(defn x [a b & args]
                     (+ a b
                        (reduce (fn [col i] (+ col i)) args)))
                       '(x 1 2 3 4)))))

(deftest test-let
  (is (= 8 (eval-js '(let [x 5
                           y 3]
                       (+ x y))))))

(deftest test-keyword-access
  (is (= 10 (eval-js '(:foo {:foo 10})))))

;; Basic Elements
(deftest test-string
  (is (= "foo" (eval-js '"foo"))))

(deftest test-number
  (is (= 10 (eval-js '10)))
  (is (= 10.0 (eval-js '10.0))))

(deftest test-identifier-cleaning
  (is (eval-js '(def one-two 12)))
  (is (eval-js '(def one? 1)))
  (is (eval-js '(def one! 1)))
  (is (eval-js '(def *one* 1))))

(deftest test-hash-map-objs
  (is (= {:foo "bar"} (eval-js '(let [asdf "foo"]
                                  {asdf "bar"})))))

;; Special Forms
(deftest test-def
  (is (= 10 (eval-js '(def x 10) 'x))))

(deftest test-fn
  (is (= 10 (eval-js '((fn [] 10))))))

(deftest test-set!
  (is (= 10 (eval-js '(def x 5)
                     '(set! x 10)
                     'x))))

(deftest test--
  (is (= 0 (eval-js '(- 3 2 1)))))

(deftest test-->
  (is (= "foobarbaz" (eval-js '(-> "foo"
                                   (str "bar")
                                   (str "baz"))))))

(deftest test-->>
  (is (= "bazbarfoo" (eval-js '(->> "foo"
                                    (str "bar")
                                    (str "baz"))))))

(deftest test-doto
  (is (= [1 2 3] (eval-js '(doto []
                             (.push 1)
                             (.push 2)
                             (.push 3))))))

(deftest test-when
  (is (eval-js '(when (> 2 1)
                  true))))

;; Scoping

;; Core Lib
(deftest test-core-count
  (is (= 3 (eval-js '(count [1 2 3])))))

(deftest test-core-first
  (is (= 1 (eval-js '(first [1 2 3])))))

(deftest test-core-rest
  (is (= [2 3] (eval-js '(rest [1 2 3])))))

(deftest test-core-reduce
  (is (= 6 (eval-js '(reduce #(+ %1 %2) [1 2 3])))))

(deftest test-core-concat
  (is (= [1 2 3 4] (eval-js '(concat [1 2] [3 4])))))

(deftest test-core-take
  (is (= [1 2] (eval-js '(take 2 [1 2 3 4]))))
  (is (= [1] (eval-js '(take 1 [1 2 3]))))
  (is (= 1 (count (eval-js '(take 1 [{:foo "bar"} {:baz "bap"}]))))))

(deftest test-core-drop
  (is (= [3 4] (eval-js '(drop 2 [1 2 3 4])))))

(deftest test-core-partition
  (is (= [3 4] (eval-js '(nth (partition 2 [1 2 3 4]) 1)))))

(deftest test-core-assoc
  (is (= "foo" (:bar (eval-js '(assoc {} :bar "foo"))))))

(deftest test-interpose
  (is (= [1 0 2 0 3] (eval-js '(interpose 0 [1 2 3])))))

(deftest test-distinct
  (is (= [1 2 3] (eval-js '(distinct [1 2 1 2 3 3 2 1])))))

(deftest test-empty?
  (is (eval-js '(empty? [])))
  (is (not (eval-js '(empty? [1 2 3]))))
  (is (eval-js '(empty? {})))
  (is (not (eval-js '(empty? {:foo "bar"})))))

(deftest test-hash-map
  (is (= {} (eval-js '(hash-map))))
  (is (= {:foo "bar"} (eval-js '(hash-map :foo "bar"))))
  (is (= {:foo "bar" :baz "bap"}
         (eval-js '(apply hash-map [:foo "bar" :baz "bap"])))))

(deftest test-apply
  (is (= 6 (eval-js '(apply + 1 [2 3]))))
  (is (= 10 (eval-js '(apply + 1 2 [3 4])))))

(deftest test-interleave
  (is (= [1 "a" 2 "b"] (eval-js '(interleave [1 2] [:a :b]))))
  (is (= [1 10 2 20 3 30] (eval-js '(interleave [1 2 3] [10 20 30 40])))))

(deftest test-keys
  (is (= ["foo-bar" "baz-bap"] (eval-js '(keys {:foo-bar 1 :baz-bap 2})))))

(deftest test-select-keys
  (is (= {:foo "bar"} (eval-js '(select-keys [:foo] {:foo "bar" :baz "bap"}))))
  (is (= {:foo "bar" :hello "world"}
         (eval-js '(select-keys [:foo :hello] {:foo "bar"
                                               :baz "bap"
                                               :hello "world"})))))


