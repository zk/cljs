(defproject testproj "1.0.0-SNAPSHOT"
  :description ""
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.2.0"]]
  :source-path "src/clj"
  :cljs {:source-path "src/cljs"
         :output-path "resources/public/js"
         :libs [{:name one-complete
                 :sources [sub.two
                           one]}
                {:name bar.main
                 :sources [bar.stuff
                           bar.core]}
                {:name ns.main}]})
