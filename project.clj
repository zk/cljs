(defproject cljs "0.1-SNAPSHOT"
  :description
  "An experimental Clojure(ish) to Javascript compiler similar to
   [clojurescript](https://github.com/clojure/clojure-contrib/tree/master/clojurescript/). The library also provides several tools to assist you with integrating cljs into your workflow.  All this in about 1k lines.  Viva Clojure!

   # Usage

   Use the `cljs.core/to-js` function to turn a list (representing some
   code) into javascript, like so:

       (to-js '((fn [x] (alert x)) \"hello world\"))

       -> function(x){alert(x);}(\"hello world\");


       (to-js '(-> ($ \"<div />\")
                (.css {:backgroundColor \"blue\" .
                       :border \"dashed white 5px\"})

       -> (function(){var out = $(\"<div />\");
                      out.css({backgroundColor:\"blue\",border:\"dashed white 5px\"});
                      return out}())

   Neat, huh?

   In addition to the compiler, cljs provides several tools to make working with cljs in the context of a web project easier:

   1. [cljs.watch](#cljs.watch) provides a mechanism for automatic recompilation on source changes. Used by either `lein cljs-watch` or `(use 'cljs.watch) (start-watch-project \"./project.clj\")`. Cljs output is declared in your project.clj file, under the :cljs key.

   2. [cljs.stitch](#cljs.stitch) takes care of stitching the collection of source files that make up a library into a cohearant javascript file.

   For more examples, please see the [cljs.test.core](#cljs.test.core) namespace."
  
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.0"]
                     [rhino/js "1.7R2"]
                     [lein-cljs "0.0.1-SNAPSHOT"]
                     [org.danlarkin/clojure-json "1.2-SNAPSHOT"]
                     [com.google.jstestdriver/jstestdriver "1.3.1"]]
  :repositories {"js-test-driver" "http://jstd-maven-plugin.googlecode.com/svn/maven2"})
