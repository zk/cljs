(ns leiningen.cljs-watch
  "# Usage
   add to your project.clj (ignore periods):

       (defproject my-project \"1.0\" .
         :cljs {:source-path \"src/cljs\" .
                :output-path \"resources/public/js\"})

   `src/cljs` and `resources/public/js` are the defaults.

   Then run `lein cljs-watch` from your project's root dir
   to enable automatic compilation of `.cljs` files when
   they are changed.

   Conversely, you can override these values from the command
   line like so:
   `$ lein cljs-watch :source-path ./path/to/cljs :output-path ./path/to/output/`."
  (:use [cljs.watch])
  (:require [clojure.java.io :as io]))

;; For REPL use
(defn parse-cljs-opts
  ([project-path]
     (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.FileReader. project-path))]
       (:cljs (apply hash-map (drop 3 (read rdr))))))
  ([] (parse-cljs-opts "project.clj")))

;; Stolen from marginalia:
(defn ls
  [path]
  (let [file (java.io.File. path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))

(defn mkdir [path]
  (.mkdirs (io/file path)))

(defn ensure-directory!
  [path]
  (when-not (ls path)
    (mkdir path)))

(defn cljs-watch [project & args]
  (let [cljs-opts (merge {:source-path "src/cljs"
                          :output-path "resources/public/js"}
                         (:cljs project)
                         (apply hash-map args))
        src-path (:source-path cljs-opts)
        out-path (:output-path cljs-opts)]
    (when-not (ls out-path)
      (println "Output directory" out-path "not found, creating.")
      (ensure-directory! out-path))
    (println "Watching" src-path "for changes...")
    (start-watch src-path out-path)))