(ns cljs.watch
  "# Utilities for automatically compiing changed .cjls files."
  (:use [cljs.core :only (compile-cljs-file)])
  (:require [clojure.string :as str]
            [cljs.compile :as compile]
            [cljs.opts :as opts]
            [clojure.contrib.io :as io]))

(defn file [file-or-path]
  (if (string? file-or-path)
    (java.io.File. file-or-path)
    file-or-path))

(defn find-cljs-files
  "Returns a seq of java.io.File objects of all .cljs files
   found in `file-or-path` including subdirs."
  [file-or-path]
  (let [f (file file-or-path)]
    (->> (file-seq f)
         (filter #(.endsWith (.getName %) ".cljs"))
         (filter #(not (re-find #"\.#" (.getName %)))))))

(defn last-mod [file]
  (.lastModified file))

(defn has-file-changed? [f last-change]
  (cond
   (nil? last-change) true
   (> (last-mod f) last-change) true
   :else false))

(defn last-mod-map [f-seq]
  (reduce #(assoc %1 %2 (last-mod %2))
          {}
          f-seq))

(def *mod-map* (atom {}))
(def *handlers* (atom []))

(defn hook-change [f]
  (swap! *handlers* (fn [hs] (conj hs f))))

(defn clear-hooks []
  (reset! *handlers* []))

(defn run-hooks [changed-files]
  (doseq [f @*handlers*]
    (f changed-files)))

(defn changed-files! [f-seq]
  (let [changed-files (doall (filter #(has-file-changed? % (@*mod-map* %)) f-seq))]
    (reset! *mod-map* (last-mod-map f-seq))
    changed-files))

(defn check-and-run! [paths]
  (let [dirs (filter #(.isDirectory (java.io.File. %))
                     paths)
        files (map #(java.io.File. %)
                   (filter #(not (.isDirectory (java.io.File. %)))
                           paths))
        cljs-files (concat
                    (reduce concat (map find-cljs-files dirs))
                    files)]
    (-> cljs-files
        (changed-files!)
        (run-hooks))))

(defn spit-equiv-js [cljs-file js-out-dir-file]
  (let [cljs-path (.getAbsolutePath cljs-file)
        name (.getName cljs-file)
        base (str/replace name #"\.cljs$" "")
        js-path (str (.getAbsolutePath js-out-dir-file) "/" base ".js")]
    (spit js-path (compile-cljs-file cljs-path))))

(defn hook-compile-out [out-dir]
  (hook-change
   (fn [cljss]
     (when (empty? cljss) #_(println "Nothing to compile"))
     (doseq [cljs cljss]
       (try
         (println "Compiling" (.getName cljs) "to" out-dir)
         (spit-equiv-js cljs (file out-dir))
         (catch Exception e (println "Problem compiling " (.getAbsolutePath cljs) ": " e)))))))

(defn hook-re-stitch [cljs-opts]
  (hook-change
   (fn [cljss]
     (if (not (empty? cljss))
       (compile/opts cljs-opts)))))

(def *run* (atom true))

(defn stop-watch []
  (reset! *run* false))

(defn start-watch [watch-path js-out-path]
  (clear-hooks)
  (reset! *run* true)
  (hook-compile-out js-out-path)
  (.start (Thread.
           (fn []
             (while @*run*
               (check-and-run! watch-path)
               (Thread/sleep 500))))))

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

(defn start-watch-opts [opts]
  (let [test-output-path (:test-output-path opts)
        source-output-path (:source-output-path opts)]
    (when (not opts)
      (throw (Exception. (str "Couldn't find cljs options in project."))))
    (clear-hooks)
    (reset! *run* true)
    (hook-re-stitch opts)
    (println "Watching" (:source-path opts) " for changes.")
    (println "Watching" (:test-path opts) " for changes.")
    (when (not (ls source-output-path))
      (println "Source output path " source-output-path "not found, creating.")
      (ensure-directory! source-output-path))
    (when (not (ls test-output-path))
      (println "Test output path " test-output-path "not found, creating.")
      (ensure-directory! test-output-path))
    (compile/opts opts)
    (doto (Thread.
                    (fn []
                      (while @*run*
                        (try
                          (check-and-run! [(:source-path opts)
                                           (:test-path opts)
                                           (when (.exists (file "./project.clj"))
                                             "./project.clj")])
                          (Thread/sleep 500)
                          (catch Exception e (println e))))))
      (.start)
      (.join))))

(defn start-watch-project
  "Starts up a watcher which will re-compile cljs files to javascript
   when cljs files found in :source-path and :test-path change.  Also re-compiles when
   your project.clj changes.

   Usage: `(start-watch-project \"./project.clj\")`"
  [& [project-path]]
  (let [opts (opts/slurp (or project-path "./project.clj"))]
    (start-watch-opts opts)))


