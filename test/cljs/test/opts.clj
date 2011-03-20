(ns cljs.test.opts
  (:use [cljs.opts] :reload)
  (:use [clojure.test]))

(deftest test-slurp-opts
  (let [{:keys [source-path
                source-output-path
                source-libs
                test-path
                test-output-path
                test-libs]}
        (slurp "dev-resources/test-project1.clj")]
    (is (= "alt-source-path" source-path))
    (is (= "alt-source-output-path" source-output-path))
    (is (= [] source-libs))
    (is (= "alt-test-path" test-path))
    (is (= "alt-test-output-path" test-output-path))
    (is (= [] test-libs))))



