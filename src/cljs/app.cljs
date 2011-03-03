(ns app
  (:use html)
  (:require [util :as u]
            [templates :as tpl])
  (:import [jQuery :as $]
           [RegExp :as RE]))

(defn keyup-timer [input on-timeout]
  (let [jqi ($ input)
        delay 300
        last-text (.val input)
        timer ('setTimeout (fn []) delay)
        handler (fn []
                  (when (not (= last-text (.val jqi)))
                    (on-timeout (.val jqi) last-text)
                    (set! last-text (.val jqi))))
        reset #(do ('clearTimeout timer)
                   (set! timer ('setTimeout handler delay)))]
    (.keyup input (fn []
                    (reset)))
    jqi))

(defn code [code-str]
  (let [ta ($html [:textarea {:class "cljs-input"} code-str])
        output ($html [:pre {:class "cljs-output"}])
        error-indicator ($html [:div {:class "error-indicator"} "!"])
        with-compiled (fn [new-js]
                        (.html output new-js)
                        (set! last-js new-js)
                        (.css error-indicator {:display "none"}))
        last-js ""]
    (.focus ta #(.fadeIn output))
    (compile code-str with-compiled)
    ($html [:div {:class "code-area"}
            (keyup-timer ta
                         (fn [n o]
                           (compile
                            n
                            with-compiled
                            #(.css error-indicator {:display "block"}))))
            output
            error-indicator])))

(defn compile [code-str with-compiled on-error]
  (ajax {:url "/compile"
         :data {:code code-str}
         :type :POST
         :success (fn [resp]
                    (with-compiled resp))
         :error on-error}))

(def code1
  (code
   "(println \"hello world\")
(println \"bar\")"))

(ready
 (fn []
   (let [body ($ "body")]
     (u/append
      body
      ($html
       [:div
        (tpl/header)
        (tpl/why)
        (tpl/features)
        (tpl/missing)
        code1])))))

(defn diff [n o]
  (-> ('diffString
       o
       n)
      (.replace (RE. " " "g") "&nbsp;") ;
      (.replace (RE. "\n" "g") "<br />")))







