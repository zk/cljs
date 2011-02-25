(ns demo.getting-started
  (:use [hiccup core [page-helpers :only (doctype)]]
        [ring.util.response]
        [net.cgrand.moustache :only (app)]
        (ring.middleware file
                         file-info
                         params
                         nested-params
                         keyword-params))
  (:require [ring.adapter.jetty :as jetty]
            [cljs.core :as cljs]))

(defn resp [body]
  (-> (response body)
      (header "Content-Type" "text/html")))

(def routes
  (app
   [""] (fn [req]
          (resp
           (html
            (doctype :html5)
            [:html
             [:head
              [:title "cljs - getting started"]
              [:style {:type "text/css"}
               "ins {background-color: #afa;}
                del {background-color: #faa;}"]
              [:script {:src "/js/jquery-1.4.4.min.js"}]
              [:script {:src "/js/underscore.min.js"}]
              [:script {:src "/js/jsdiff.js"}]
              [:script {:src "/js/app.js"}]]])))
   ["compile"] (fn [req]
                 (resp (cljs/compile-cljs-string (:code (:params req)))))))

(def entry-handler
  (-> routes
      (wrap-keyword-params)
      (wrap-nested-params)
      (wrap-params)
      (wrap-file "resources/public")
      (wrap-file-info)))

(def server nil)

(defn stop []
  (when server
    (.stop server)))

(defn start [entry-handler & [port]]
  (stop)
  (alter-var-root
   (var server)
   (fn [val]
     (jetty/run-jetty entry-handler {:port (if port port 8080) :join? false}))))

#_(start (var entry-handler) 8080)
