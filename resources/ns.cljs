(ns foo.bar
  (:use use-import)
  (:require require-import))

(def x 5)

(defn add-5 [y] (+ x y))
