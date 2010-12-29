(ns cljs.watch
  "# Utilities for automatically compiing changed .cjls files."
  (:use [cljs.core :only (compile-cljs compile-to)])
  (:require [clojure.string :as str]))

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
         (filter #(.endsWith (.getName %) ".cljs")))))

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

(defn check-and-run! [path]
  (-> (find-cljs-files path)
      (changed-files!)
      (run-hooks)))

(defn spit-equiv-js [cljs-file js-out-dir-file]
  (let [cljs-path (.getAbsolutePath cljs-file)
        name (.getName cljs-file)
        base (str/replace name #"\.cljs$" "")
        js-path (str (.getAbsolutePath js-out-dir-file) "/" base ".js")]
    (compile-to cljs-path js-path)))

(defn hook-compile-out [out-dir]
  (hook-change
   (fn [cljss]
     (when (empty? cljss) #_(println "Nothing to compile"))
     (doseq [cljs cljss]
       (try
         (println "Compiling" (.getAbsolutePath cljs) "to" out-dir)
         (spit-equiv-js cljs (file out-dir))
         (catch Exception e (println "Problem compiling " (.getAbsolutePath cljs) ": " e)))))))


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
