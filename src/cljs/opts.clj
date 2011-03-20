(ns cljs.opts
  "Responsible for parsing cljs options from a variety of sources, such as:

   * Leiningen project.clj
   * Cake project.clj

   See var `defaults` for default options. "
  (:use [clojure.java.io :only (file)])
  (:import [java.io File FileReader]
           [clojure.lang LineNumberingPushbackReader]))

(def defaults
  {:source-path "src/cljs"
   :source-output-path "resources/public/js"
   :source-libs []
   :test-path "test/cljs"
   :test-output-path "resources/testjs"
   :test-libs []})


(defn slurp [file-or-path]
  (let [file (file file-or-path)
        rdr (LineNumberingPushbackReader. (FileReader. file))]
    (->> (read rdr)
         (drop 5)
         (apply hash-map)
         :cljs
         (merge defaults))))
