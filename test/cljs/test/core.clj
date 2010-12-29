(ns cljs.test.core
  (:use [cljs.core] :reload)
  (:use [clojure.test]))


;; # Interesting Examples
;;
;; Start here if you'd like to get an idea of what
;; **cljs** is capable of.


;; This will (with jquery) create a `<div />`, set it's text to "Click Me!",
;; set it's dimensions to 100x100, set it's background color to red, pop up
;; an alert box, with the message "I was clicked", when clicked, and insert
;; it as a child of `<body />`.
;;
;; See the example [here](http://zkim.github.com/cljs/examples/red-clickable-box.html).
(println (map js '(

                   (defn click-handler []
                     (alert "I was clicked!"))

                   (defn body [] ($ "body"))

                   (defn clickable-div []
                     (doto ($ "<div />")
                         (.click click-handler)
                         (.css {:width 100
                                :height 100
                                :backgroundColor "red"})
                         (.append "Click Me!")))

                   (.ready ($ document)
                           (fn []
                             (.append (body) (clickable-div))))
                   
                   )))

(deftest test-handle-println
  (is (= "console.log(\"hello world\")" (handle-println '(println "hello world")))))


;; # Low-Level Converters

(deftest test-convert-map
  (is (= "{'hello':\"world\"}" (convert-map {:hello "world"}))))

(deftest test-convert-string
  (is (= "\"hello world\"" (convert-string "hello world"))))

(deftest test-convert-number
  (is (= "5" (convert-number 5))))

(deftest test-convert-vector
  (is (= "[1,2,3]" (convert-vector [1 2 3]))))

(deftest test-convert-symbol
  (is (= "hello_world" (convert-symbol 'hello-world)))
  (is (= "hello.world" (convert-symbol 'hello/world))))

(convert-symbol 'hello/world)

(deftest test-emit-function
  (is (= "function(x,y){\n5;\n6;\nreturn 7;\n}" (emit-function '[x y] '(5 6 7)))))

(deftest test-convert-dot-function
  (is (= "x.stuff(1,2,3)" (convert-dot-function '(.stuff x 1 2 3)))))

(deftest test-convert-plain-function
  (is (= "stuff(1,2,3)" (convert-plain-function '(stuff 1 2 3)))))

(deftest test-convert-function
  (is (= "x.stuff(1,2,3)" (convert-function '(.stuff x 1 2 3))))
  (is (= "stuff(1,2,3)" (convert-function '(stuff 1 2 3)))))

(deftest test-handle-if
  (is (= "(function(){if((x==1)){return x;}})()" (handle-if '(if (= x 1) x)))))

#_(js '(let [y 5] (println y)))




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
              (doto ($ "body")
                  (.css {:backgroundColor "red"})
                  (.append ($ "<h1>HI!</h1>")))))

  (js-form '(dostuff (doto ($ "<div />")
                         (.append ($ "<span />")))))

  (js-form '(defn log-stuff [s] (println [1 2 s])))

  (js-form '[1 2 3])

  (doc read)

  (js-form '(defn add-5 [x]
              (+ 5 x)
              (add-5 5)))


  (compile-cljjs-to "./resources/public/cljjs/test.clj.js" "./resources/public/js/test.js")

)