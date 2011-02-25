(ns app
  (:use util html)
  (:import [jQuery :as $]
           [RegExp :as RE]))

(defn keyup-timer [input on-timeout]
  (let [jqi ($ input)
        delay 300
        timer ('setTimeout (fn []) delay)
        handler #(on-timeout (.val jqi))
        reset #(do ('clearTimeout timer)
                   (set! timer ('setTimeout handler delay)))]
    (.keyup input (fn []
                    (reset)))
    jqi))

(defn ajax [opts]
  (.ajax $ opts))

(defn main-view []
  (let [last-text ""
        output ($html [:div {:style "border: solid black 1px;"}])
        input (keyup-timer ($html [:textarea])
                           #(ajax {:url "/compile"
                                   :data {:code %}
                                   :type :POST
                                   :success (fn [resp]
                                              (.html output (-> ('diffString
                                                                 last-text
                                                                 resp)
                                                                (.replace (RE. " " "g") "&nbsp;")
                                                                (.replace (RE. "\n" "g") "<br />")))
                                              (set! last-text resp))}))]
    ($html [:div
            "Main View"
            input
            output])))



(ready
 #(let [body ($ "body")]
    (append body (main-view))))
