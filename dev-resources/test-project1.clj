(defproject test-project1 "1.0.0-SNAPSHOT"
  :description ""
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.0"]
                     [cljs "0.1-SNAPSHOT"]
                     [lein-cljs "0.1-SNAPSHOT"]
                     [cljs-contrib "0.1-SNAPSHOT"]]
  :source-path "src/clj" 
  :cljs {:source-path "alt-source-path"
         :source-output-path "alt-source-output-path"
         :test-path "alt-test-path"
         :test-output-path "alt-test-output-path"})
