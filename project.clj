(defproject cljs "0.0.1-SNAPSHOT"
  :description
  "An experimental Clojure(ish) to Javascript compiler similar to
   [clojurescript](https://github.com/clojure/clojure-contrib/tree/master/clojurescript/).

   # Usage

   Use the `cljs.core/js` function to turn a list (representing some
   code) into javascript, like so:

       (js '((fn [x] (alert x)) \"hello world\"))

       -> function(x){alert(x);}(\"hello world\");


       (js '(-> ($ \"<div />\")
                (.css {:backgroundColor \"blue\" .
                       :border \"dashed white 5px\"})

       -> (function(){var out = $(\"<div />\");
                      out.css({backgroundColor:\"blue\",border:\"dashed white 5px\"});
                      return out}())

   Neat, huh?

   For more examples, please see the [cljs.test.core](#cljs.test.core) namespace."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.0"]
                     [marginalia "0.2.2"]
                     [rhino/js "1.7R2"]])
