(ns cljs.deps
  (:use [clojure.java.io :only (file)])
  (:require [clojure.string :as str])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io FileReader]))

(defn read-ns-form [reader]
  (with-open [rdr (LineNumberingPushbackReader. reader)]
    (let [ns-decl (first (filter #(= 'ns (first %)) (repeatedly #(read rdr))))]
      ns-decl)))

(defn retr-ns-form
  "Check the filesystem and classpath for `file-or-path` and
   returns the namespace form if found, nill if not."
  [file-or-path]
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
   as namespace symbols.

   Usage: (extract-namespaces :use '(ns foo (:use bar)))"
  [tag ns-form]
  (->> ns-form
       (filter coll?)
       (filter #(= tag (first %)))
       (map #(drop 1 %))
       (reduce concat)
       (map #(if (coll? %)
               (first %)
               %))))

(defn parse-ns-form
  "Parses name, uses, and requires from a ns form."
  [ns-form]
  (when ns-form
    {:name (second ns-form)
     :uses (extract-namespaces :use ns-form)
     :requires (extract-namespaces :require ns-form)}))

(defn find-namespace
  "Looks in coll search-paths and classpath for file named by `ns-sym`.
   Returns a map like so:

       {:name foo
        :file \"path/to/foo.cljs\"
        :uses (lib.bar)
        :requires (lib.baz)}"
  [search-paths ns-sym]
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
  "Analyzes a cljs lib for dependencies.  Used by compile to collect
   the dependencies for a library given by `ns-name` and found in either
   `search-paths` or on the classpath."
  (let [ns (find-namespace search-paths ns-name)
        deps (resolve-deps search-paths (concat (:uses ns) (:requires ns)))]
    (when ns
      (assoc ns :deps deps))))


