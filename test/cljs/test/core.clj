(ns cljs.test.core
  (:use [cljs.core] :reload)
  (:use [clojure.test]))


;; # Interesting Examples
;;
;; Start here if you'd like to get an idea of what
;; **cljs** is capable of.

(deftest test-handle-println
  (is (= "console.log(\"hello world\")" (handle-println '(println "hello world")))))


;; # Low-Level Converters

(deftest test-convert-map
  (is (= "{hello:\"world\"}" (convert-map {:hello "world"}))))

(deftest test-convert-string
  (is (= "\"hello world\"" (convert-string "hello world"))))

(deftest test-convert-number
  (is (= "5" (convert-number 5))))

(deftest test-convert-vector
  (is (= "[1,2,3]" (convert-vector [1 2 3]))))

(deftest test-convert-symbol
  (is (= "hello_world" (convert-symbol 'hello-world))))

(deftest test-emit-function
  (is (= "function(x,y){\n5;\n6;\nreturn 7;\n}" (emit-function '[x y] '(5 6 7)))))

(deftest test-convert-anon-fn
  (is (= "function(x){\nreturn 5;\n}" (convert-anon-fn '(fn [x] 5)))))

(deftest test-convert-dot-function
  (is (= "x.stuff(1,2,3)" (convert-dot-function '(.stuff x 1 2 3)))))

(deftest test-convert-plain-function
  (is (= "stuff(1,2,3)" (convert-plain-function '(stuff 1 2 3)))))

(deftest test-convert-function
  (is (= "x.stuff(1,2,3)" (convert-function '(.stuff x 1 2 3))))
  (is (= "stuff(1,2,3)" (convert-function '(stuff 1 2 3)))))


(comment

  

  (js-form '(fn [x] [1 2 x 3]))

  (js-form '(def x (fn [] (println "hi"))))

  (js-form '(defn hello [a b]
              (println a)
              (println b)))

  (js-form '(defn h1 [x]
              (println "HI!")
              (str "<h1>" x "</h1>")))

  (println (js-form '(fn [x y] (println "hello world"))))

  (println (js-form '(println "hello world")))

  (js-form '(println ($ "#")))

  (js-form '($ "#hello world"))

  (println (js-form '{:hello "world"
                      :stuff (fn [x] (println "hi"))
                      :yo "hi"}))

  (println (js-form '(.click ($ "#body")
                             (fn [] (println "on click"))
                             (fn [x] (alert x)))))

  (println (js-form '(.ajax $ {:success (fn [res] (println res))
                               :failure (fn [res] (println "ERROR!"))})))

  (js-form '(let [x 5]
              (println x)))

  (spit "./resources/public/js/jsclj-test.js"
        (js-form
         '(.ready ($ document)
                  (fn []
                    (println "hi")
                    (.click ($ "body")
                            (fn []
                              (alert "hi")))))))

  (js-form
   '(.ready ($ document)
            (fn [] (println "hi"))))

  (js-form
   '(.ready ($ document)
            (fn [] (println "hi"))))

  (def x '($ "#hi"))

  (js-form x)

  (js-form '(defn onready []
              (-> ($ "body")
                  (.css {:backgroundColor "red"})
                  (.append ($ "<h1>HI!</h1>")))))

  (js-form '(dostuff (-> ($ "<div />")
                         (.append ($ "<span />")))))

  (js-form '(defn log-stuff [s] (println [1 2 s])))

  (js-form '[1 2 3])

  (doc read)

  (js-form '(defn add-5 [x]
              (+ 5 x)
              (add-5 5)))


  (compile-cljjs-to "./resources/public/cljjs/test.clj.js" "./resources/public/js/test.js")

)