(ns cljs.test.watch
  (:use [cljs.watch] :reload)
  (:use [clojure.test]))

(deftest test-find-cljs-files
  (is (empty? (filter #(re-find #"\.#" (.getName %))
                      (find-cljs-files "./resources")))))

