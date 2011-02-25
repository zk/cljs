(ns util
  (:import [jQuery :as $]
           RegExp))

(defn has-el? [o]
  (when o
    (aget o :el)))

(defn append [p c]
  (cond
   (array? c) (map (fn [c] (append p c)) c)
   (has-el? c) (append p (:el c))
   :else (do
           (.append p c)
           (when (instanceof c 'jQuery)
             (.trigger c "postinsert"))))
  p)

(defn replace-in [p c]
  (.empty p)
  (append p c))

(defn take [n o]
  (cond
   (string? o) (.substring o 0 n)))

(defn h1? [el]
  (= 'el.type "h1"))

(defn paragraph? [el]
  (= 'el.type "paragraph"))

(defn run? [el]
  (and (not (= 'null el)) (= 'el.type "run")))

(defn ordered-list? [el]
  (and (not (= 'null el)) (= 'el.type "ordered-list")))

(defn run-content [el]
  (if el
    'el.content))

(defn text-content [el]
  (cond
   (h1? el) (apply str (map run-content 'el.content))))

(defn apply-str [ss]
  (reduce (fn [col s] (+ col s)) ss))

(defn ellipsis [s n]
  (if (> 's.length n)
    (str (.substring s 0 n) "...")
    s))

(def loading-indicator ($ "#loading-indicator"))

(defn loading [enable]
  (if enable
    (.css loading-indicator {:display "block"})
    (.hide loading-indicator {:display "none"})))

(.ready ($ 'document)
       #(set! loading-indicator ($ "#loading-indicator")))

(defn make-url-friendly [s]
  (-> s
      (.replace (RegExp. "-" "g") "_")
      (.replace (RegExp. " " "g") "_")
      (.toLowerCase)))

(defn ready [f]
  (.ready ($ 'document) f))

(defn has-layout? [o]
  (when o
    (aget o :layout)))

(defn ajax [opts]
  (.ajax $ opts))
