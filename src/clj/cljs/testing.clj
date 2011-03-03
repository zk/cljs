(ns cljs.testing
  (:require [clojure.string :as str])
  (:import (com.google.jstestdriver JsTestDriver
                                    PluginLoader)
           (com.google.jstestdriver.config CmdFlags
                                           CmdLineFlagsFactory
                                           YamlParser)))




(comment
  (use 'clojure.pprint)

  (defn run [args-str]
    (let [flags-fac (CmdLineFlagsFactory.)
          flags (.create flags-fac (into-array String (str/split args-str #"\s+")))
          base-path (.getBasePath flags)
          plugins (.getPlugins flags)
          ploader (PluginLoader.)
          pmodules (.load ploader plugins)
          imodules pmodules
          config (.parse (YamlParser.)
                         (java.io.FileReader.
                          (let [cs (.getConfigurationSource flags)
                                pf (.getParentFile cs)
                                name (.getName cs)]
                            (java.io.File. (str (.getAbsolutePath pf)
                                                "/"
                                                name)))))
          ]
      (pprint config)))


  (run "--port 4224 --config ./.jstestdriver")


  #_(JsTestDriver/main (into-array String ["--port" "4224"]))

  #_(JsTestDriver/main (into-array String ["--tests" "all" "--config" "./.jstestdriver"])))




