(ns leiningen.cljs-compile
  (:require [cljs.stitch :as stitch]))

(defn cljs-compile [project]
  (stitch/stitch-project "./project.clj"))
