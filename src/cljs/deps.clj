(ns cljs.deps
  (:use [clojure.java.io :only (file)]
        [clojure.contrib.seq :only (find-first)])
  (:require [clojure.string :as str])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io FileReader]))


(defn read-ns-form [reader]
  (with-open [rdr (LineNumberingPushbackReader. reader)]
    (let [ns-decl (find-first #(= 'ns (first %)) (repeatedly #(read rdr)))]
      ns-decl)))

(defn retr-ns-form [file-or-path]
  (let [file (file file-or-path)]
    (if (.exists file)
      (read-ns-form (FileReader. file))
      (try
        (-> (.getContextClassLoader (Thread/currentThread))
            (.getResourceAsStream (.getPath file))
            (java.io.InputStreamReader.)
            (read-ns-form))
        (catch Exception e nil)))))

(defn extract-namespaces
  "Given a ns and a tag (i.e. :use or :require) collect the dependencies
   as namespace symbols."
  [tag ns-form]
  (->> ns-form
       (filter coll?)
       (filter #(= tag (first %)))
       (map #(drop 1 %))
       (reduce concat)
       (map #(if (coll? %)
               (first %)
               %))))

(defn parse-ns-form [ns-form]
  (when ns-form
    {:name (second ns-form)
     :uses (extract-namespaces :use ns-form)
     :requires (extract-namespaces :require ns-form)}))

(defn find-namespace [search-paths ns-sym]
  (let [file-name (str (str/replace (str ns-sym) #"\." "/")
                       ".cljs")
        guesses (concat (map #(str % "/" file-name) search-paths)
                        [file-name])]
    (loop [guesses guesses]
      (if (= 0 (count guesses))
        nil
        (if-let [res (parse-ns-form (retr-ns-form (first guesses)))]
          (assoc res :file (first guesses))
          (recur (rest guesses)))))))


(defn resolve-deps [search-paths ns-coll]
  "Given a list of search paths and a list of namespaces representing
   dependencies, recursively resolve namespaces to files.

   Results are in the format: `{:name foo.bar :file src/foo/bar.cljs}`"
  (when (not (empty? ns-coll))
    (let [deps (->> ns-coll
                    (map #(find-namespace search-paths %)))
          dep-deps (reduce #(concat %1 (:uses %2) (:requires %2)) [] deps)]
      (distinct
       (concat (resolve-deps search-paths dep-deps)
               (map #(select-keys % [:name :file]) deps))))))

(defn analyze [search-paths ns-name]
  (let [ns (find-namespace search-paths ns-name)
        deps (resolve-deps search-paths (concat (:uses ns) (:requires ns)))]
    (when ns
      (assoc ns :deps deps))))


