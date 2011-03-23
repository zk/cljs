(ns cljs.compile
  (:use clojure.pprint
        [clojure.java.io :only (file)])
  (:require [cljs.opts :as opts]
            [cljs.deps :as deps]
            [cljs.core :as core])
  (:import [clojure.lang LispReader$ReaderException]))

;; There has to be a better way than inspecting the message
;; content
(defn error [e]
  (let [message (.getMessage e)]
    (cond
     (re-find #"EOF while reading" message)
     (do (println "*** ERROR ***  EOF while reading file, are you missing closing parens?"))
     :else (do
             (println "*** ERROR *** " message)
             (.printStackTrace e)))))

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


;; Why pass all this duplicate info?!
(defn libs [opts search-paths libs]
  (doseq [source-lib libs]
    (let [analyzed (deps/analyze search-paths source-lib)]
      (println "*" (:name analyzed) "--" (:file analyzed))
      (println)
      (when (:deps analyzed)
        (println "  deps")
        (doseq [dep (:deps analyzed)]
          (println "   " (:name dep) "--" (:file dep))))
      (print "  Compiling...  ")
      (try
        (let [compiled (lib opts analyzed)
              out-path (str (:source-output-path opts) "/" (:name analyzed) ".js")]
          (println "done.  ")
          (print "  Writing to" (str out-path "...  "))
          (spit out-path compiled)
          (println "done.")
          (println))
        (catch Exception e (error e))))))

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


