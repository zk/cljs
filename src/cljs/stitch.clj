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
   into `resources/public/js/out-name.js`.
"
  (:use [clojure.contrib.string :only (as-str)]
        [clojure.contrib.find-namespaces :only (read-ns-decl)]
        [clojure.contrib.seq :only (find-first)]
        [clojure.pprint :only (pprint)])
  (:require [clojure.string :as str]
            [cljs.core :as core]))

(defn cljs-opts
  "Pulls the :cljs map from the specified `project-clj`."
  [project-clj]
  (try
    (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.FileReader. project-clj))]
      (->> (read rdr)
           (drop 5)
           (apply hash-map)
           :cljs))
    (catch Exception e
      (println (str "Problem reading project at " project-clj)))))

(defn ns-to-file [source-root ns-sym]
  (str
   source-root
   "/"
   (str/replace (str ns-sym) #"\." "/")
   ".cljs"))

(defn ns-decl [file]
  (with-open [rdr (clojure.lang.LineNumberingPushbackReader.
                   (java.io.FileReader. file))]
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



(defn find-dependencies [source-root source-file]
  (let [ns-decl (ns-decl source-file)
        includes (distinct (includes ns-decl))]
    (distinct
     (flatten
      (map #(concat (find-dependencies source-root (ns-to-file source-root %)) [%])
           includes)))))


(defn stitch-lib
  "Converts and stitches `sources` into `output-path/name.js`."
  [source-path output-path name]
  (println "Stitching" name)
  (println "  " "input:")

  (println "  " "output:")
  (println "    "  (str output-path "/" name ".js"))

  (->> (concat (find-dependencies source-path (ns-to-file source-path name))
               [name])
       (#(do (println %) %))
       (map #(ns-to-file source-path %))
       (map core/compile-cljs-file)
       (interpose "\n\n\n\n")
       (apply str)
       (str core/*core-lib* "\n\n\n\n")
       (spit (str output-path "/" (str name) ".js"))))

(defn stitch-lib-with-sources
  "Converts and stitches `sources` into `output-path/name.js`."
  [source-path output-path name sources]
  (let [cljs-source-paths (->> sources
                               (map str)
                               (map #(str/replace % #"\." "/"))
                               (map #(str source-path "/" % ".cljs")))]
    (println "Stitching" name)
    (println "  " "input:")

    (doseq [sp cljs-source-paths]
      (println "    " sp)
      (when (not (.exists (java.io.File. sp)))
        (throw (Exception.
                (str "Couldn't find source path "
                     sp
                     " for library "
                     name)))))

    (println "  " "output:")
    (println "    "  (str output-path "/" name ".js"))

    (->> cljs-source-paths
         (map core/compile-cljs-file)
         (interpose "\n\n\n\n")
         (apply str)
         (str core/*core-lib* "\n\n\n\n")
         (spit (str output-path "/" (str name) ".js")))))


(defn stitch-libs [output-path source-path libs]
  (doseq [lib libs]
    (if (map? lib)
      (stitch-lib-with-sources source-path output-path (as-str (:name lib)) (:sources lib))
      (stitch-lib source-path output-path (as-str lib)))))

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
         output-path (prefix-path
                      (:project-root opts)
                      (:output-path copts))
         source-path (prefix-path
                      (:project-root opts)
                      (:source-path copts))
         libs (:libs copts)]
     (println "Stitching libs with")
     (pprint copts)
     (println)
     (stitch-libs output-path source-path libs))))


#_(find-dependencies "./resources/testproj/src/cljs"
                   "./resources/testproj/src/cljs/ns/main.cljs")

#_(stitch-project "./resources/testproj/project.clj" :project-root "./resources/testproj")
