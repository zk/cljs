(ns leiningen.cljs-install
  (:require [leiningen.install]
            [leiningen.jar]
            [leiningen.cljs-jar]))

(defn cljs-install [project]
  (binding [leiningen.jar/jar leiningen.cljs-jar/cljs-jar]
    (leiningen.install/install project)))
