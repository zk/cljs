(ns cljs.rhino
  "Used for testing / debugging of generated javascript."
  (:import (org.mozilla.javascript Context
                                   Scriptable)))

(defn slurp-resource
  [resource-name]
  (-> (.getContextClassLoader (Thread/currentThread))
      (.getResourceAsStream resource-name)
      (java.io.InputStreamReader.)
      (slurp)))

(def underscore-js-source (slurp-resource "underscore.js"))

(defn eval-js [snippet]
  (let [cx (Context/enter)
        sc (.initStandardObjects cx)]
    (try
      (.evaluateString cx sc "var console = {}; console.log = function(){};" "<cmd>" 1 nil)
      (.evaluateString cx sc underscore-js-source "underscore.js" 1 nil)
      (.evaluateString cx sc snippet "<cmd>" 1 nil)
      (finally (Context/exit)))))

(defn objtomap [obj]
  (let [obj-ids (seq (.getIds obj))
        vals (map #(.get obj % nil) obj-ids)
        keys (map keyword obj-ids)]
    (apply hash-map (interleave keys vals))))

#_(objtomap (eval-js (slurp "./resources/scope/test1.js")))

#_(eval-js (slurp "./resources/scope/test1.js"))


