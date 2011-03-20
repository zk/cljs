(ns cljs.compile
  (:use clojure.pprint)
  (:require [cljs.opts :as opts]
            [cljs.deps :as deps]
            [cljs.core :as core]))


(defn lib [opts analyzed-lib]
  (let [dep-files (map :file (:deps analyzed-lib))
        lib-file (:file analyzed-lib)]
    (->> (concat dep-files [lib-file])
         (map core/compile-cljs-file)
         (interpose "\n\n\n\n")
         (apply str)
         (str core/*core-lib*))))

(defn libs [opts search-paths libs]
  (doseq [source-lib libs]
    (let [analyzed (deps/analyze search-paths source-lib)]
      (println "  Lib:")
      (println "   " (:name analyzed) "--" (:file analyzed))
      (println "  Deps:")
      (doseq [dep (:deps analyzed)]
        (println "   " (:name dep) "--" (:file dep)))
      (print "  Compiling...  ")
      (let [compiled (lib opts analyzed)
            out-path (str (:source-output-path opts) "/" (:name analyzed) ".js")]
        (println "done.  ")
        (print "  Writing to" (str out-path "...  "))
        (spit out-path compiled)
        (println "done.")))))

(defn opts [opts]
  (time
   (let [source-libs (:source-libs opts)
         source-search-paths [(:source-path opts)]]
     (println)
     (println "-------------------------------")
     (println "Compiling cljs libs:")
     (pprint opts)
     (println)
     (println "Source Libraries:")
     (libs opts source-search-paths source-libs)
     (println)
     (println "Test Libraries:")
     (libs opts
           [(:source-path opts)
            (:test-path opts)]
           (:test-libs opts))
     (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
     (println (java.util.Date.))))
  (println))

(defn project [& [project-file]]
  (opts (opts/slurp (or project-file "./project.clj"))))

