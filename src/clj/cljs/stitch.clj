(ns cljs.stitch
  "Stitch is responsible for collecting disparate cljs source files into
   a single js library.  Js libs are specified in your project.clj file
   like so:

       (defproject foo \"1.0\"
         :dependencies ...
         :cljs {:source-path \"./src/cljs\"
                :output-path \"./resources/public/js\"
                :libs [{:name :out-name
                        :sources [:one
                                  :subdir.two]}]})

   Stich will stich together `src/cljs/one.cljs` and `src/cljs/subdir/two.cljs`
   into `resources/public/js/out-name.js`."
  (:use [clojure.contrib.string :only (as-str)]
        [clojure.contrib.find-namespaces :only (read-ns-decl)]
        [clojure.contrib.seq :only (find-first)]
        [clojure.pprint :only (pprint)])
  (:require [clojure.string :as str]
            [cljs.core :as core]))

(defn cljs-opts
  "Returns the value from the :cljs key in a project map. Accepts both a project map or
   path to project.clj."
  [path-or-map]
  (if (map? path-or-map)
    (:cljs path-or-map)
    (try
      (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.FileReader. path-or-map))]
        (->> (read rdr)
             (drop 5)
             (apply hash-map)
             :cljs))
      (catch Exception e
        (println (str "Problem reading project at " path-or-map))))))

(defn ns-to-path [source-root ns-sym]
  (str
   source-root
   "/"
   (str/replace (str ns-sym) #"\." "/")
   ".cljs"))

(defn ns-decl [source]
  (with-open [rdr (clojure.lang.LineNumberingPushbackReader.
                   (java.io.StringReader. source))]
    (let [ns-decl (find-first #(= 'ns (first %)) (repeatedly #(read rdr)))]
      ns-decl)))


(defn includes [ns-decl]
  (->> ns-decl
       (filter #(or (seq? %) (vector? %)))
       (filter #(or (= :use (first %))
                    (= :require (first %))))
       (map #(drop 1 %))
       (reduce concat)
       (map #(if (or (seq? %) (vector? %))
               (first %)
               %))))

(defn cljs-source-for [search-paths name]
  (let [res (->> search-paths
                 (map (fn [path]
                        (let [file (java.io.File. (ns-to-path path name))]
                          (if (.exists file)
                            (slurp (.getAbsolutePath file))
                            (try
                              (-> (.getContextClassLoader (Thread/currentThread))
                                  (.getResourceAsStream (str (str/replace name #"\." "/") ".cljs"))
                                  (java.io.InputStreamReader.)
                                  (slurp))
                              (catch Exception e nil))))))
                 (filter identity)
                 (first))]
    (if res
      res
      (println (str "Couldn't find " name " in "
                    (apply str (interpose ", " search-paths))
                    " or classpath.")))))


(defn find-dependencies [search-paths cljs-source]
  (let [ns-decl (ns-decl cljs-source)
        includes (distinct (includes ns-decl))]
    (distinct
     (flatten
      (map #(concat (find-dependencies search-paths (cljs-source-for search-paths %)) [%])
           includes)))))


(defn stitch-lib
  "Converts and stitches `sources` into `output-path/name.js`."
  [search-paths output-path name]
  (println "Stitching" name)
  (let [source (cljs-source-for search-paths name)]
    (if (not source)
      (println "Skipping" name)
      (let [deps (concat (find-dependencies search-paths source)
                         [name])]
        (println "  " "input:")
        (println "    " (apply str (interpose "\n     " deps)))

        (println "  " "output:")
        (println "    "  (str output-path "/" name ".js"))
        (println)

        (->> deps
             (map #(cljs-source-for search-paths %))
             (map core/compile-cljs-string)
             (interpose "\n\n\n\n")
             (apply str)
             (str core/*core-lib* "\n\n\n\n")
             (spit (str output-path "/" (str name) ".js")))))))

(defn stitch-lib-with-sources
  "Converts and stitches `sources` into `output-path/name.js`. Looks for
   sources in the filesystem first at `source-path`/`name`, then does a
   classpath lookup."
  [source-path output-path name sources]
  (try
    (let [cljs-source-paths (->> sources
                                 (map str)
                                 (map #(str/replace % #"\." "/"))
                                 (map #(str source-path "/" % ".cljs")))]
      (println "Stitching" name)
      (println "  " "input:")


      (println "  " "output:")
      (println "    "  (str output-path "/" name ".js"))
      (println)

      (->> sources
           (map #(cljs-source-for source-path %))
           (map core/compile-cljs-string)
           (interpose "\n\n\n\n")
           (apply str)
           (str core/*core-lib* "\n\n\n\n")
           (spit (str output-path "/" (str name) ".js"))))))


(defn stitch-libs [output-path search-paths libs]
  (doseq [lib libs]
    (if (map? lib)
      (stitch-lib-with-sources search-paths output-path (as-str (:name lib)) (:sources lib))
      (stitch-lib search-paths output-path (as-str lib)))))

(defn prefix-path [prefix path]
  (str
   (when prefix
     prefix)
   (when prefix
     "/")
   path))

(defn stitch-project [project-clj-path & opts]
  (time
   (let [opts (apply hash-map opts)
         copts (cljs-opts project-clj-path)
         source-output-path (prefix-path
                             (:project-root opts)
                             (:source-output-path copts))
         source-path (prefix-path
                      (:project-root opts)
                      (:source-path copts))
         source-libs (:source-libs copts)

         test-output-path (prefix-path
                           (:project-root opts)
                           (:test-output-path copts))
         test-path (prefix-path
                    (:project-root opts)
                    (:test-path copts))
         test-libs (:test-libs copts)]
     (println)
     (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
     (println "vvv" "cljs project options" "vvv")
     (pprint copts)
     (println)
     (stitch-libs source-output-path [source-path] source-libs)
     (stitch-libs test-output-path [source-path test-path] test-libs)
     (println "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
     (println (java.util.Date.)))))


