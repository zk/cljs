(defproject cljs-test-project "1.0.0-SNAPSHOT"
  :description "cljs test project"
  :dependencies []
  :dev-dependencies []
  :source-path "src/clj"
  :cljs {:source-path "dev-resources/test-project/src"
         :source-output-path "dev-resources/test-project/resources/js"
         :source-libs [app]
         :test-path "dev-resources/test-project/test"
         :test-output-path "dev-resources/test-project/resources/testjs"
         :test-libs [app-test]})
