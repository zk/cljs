(ns cljs.test.deps
  (:use [cljs.deps] :reload)
  (:use [clojure.test]))

(deftest test-extract-namespaces
  (is (= '(bar baz bap)
         (extract-namespaces :use '(ns foo
                                     (:use bar baz bap)))))
  (is (= '(asdf qwer zxcv) (extract-namespaces
                            :require
                            '(ns bar
                               (:require asdf qwer [zxcv :as yay]))))))

(deftest test-analyze
  (let [res (analyze ["dev-resources/test-sources"] 'app)]
    (is (= 3 (count (:deps res))))
    (is (= "dev-resources/test-sources/app.cljs" (:file res)))
    (is (= 'app (:name res)))
    (is (= '(sub.one sub.two) (:uses res)))
    (is (= '(common) (:requires res)))))



