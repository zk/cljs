(ns cljs.compile
  (:use clojure.pprint
        [clojure.java.io :only (file)])
  (:require [cljs.opts :as opts]
            [cljs.deps :as deps]
            [cljs.core :as core]))

(defn cljs-source
  "Returns `path` content as string.  Checks both
   filesystem and classpath."
  [path]
  (let [file (file path)]
    (if (.exists file)
      (slurp file)
      (try
        (-> (.getContextClassLoader (Thread/currentThread))
            (.getResourceAsStream (.getPath file))
            (java.io.InputStreamReader.)
            (slurp))
        (catch Exception e nil)))))

(defn lib [opts analyzed-lib]
  (let [dep-files (map :file (:deps analyzed-lib))
        lib-file (:file analyzed-lib)]
    (->> (concat dep-files [lib-file])
         (map cljs-source)
         (map core/compile-cljs-string)
         (interpose "\n\n\n\n")
         (apply str)
         (str core/*core-lib*))))

(defn libs [opts search-paths libs]
  (doseq [source-lib libs]
    (let [analyzed (deps/analyze search-paths source-lib)]
      (println "*" (:name analyzed) "--" (:file analyzed))
      (println)
      (println "  deps")
      (doseq [dep (:deps analyzed)]
        (println "   " (:name dep) "--" (:file dep)))
      (print "  Compiling...  ")
      (let [compiled (lib opts analyzed)
            out-path (str (:source-output-path opts) "/" (:name analyzed) ".js")]
        (println "done.  ")
        (print "  Writing to" (str out-path "...  "))
        (spit out-path compiled)
        (println "done.")
        (println)))))

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
     (println)
     (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
     (println (java.util.Date.))))
  (println))

(defn project [& [project-file]]
  (opts (opts/slurp (or project-file "./project.clj"))))
