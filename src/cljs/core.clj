(ns cljs.core
  "Experimental, very WIP so use at your own risk."
  (:require [clojure.string :as str]))

;; # Low-Level Conversion Routines
;;
;; The convert-* functions take in some clojure element
;; and return a string representing the
;; javascript equivalent.

(declare convert-el)
(declare convert-function)

(def *current-ns* (atom nil))

(defn cns []
  (-> (str @*current-ns*)
      (str/replace #"\." "_DOT_")))

(defn set-current-ns [ns]
  (reset! *current-ns* ns))

(defn cns-with-dot []
  (if (not (empty? (cns)))
    (str (cns) ".")))

(defn add-return [statements]
  (let [count (dec (count statements))
        before-ret (take count statements)
        after-ret (drop count statements)
        with-return (concat before-ret [(apply str "return " after-ret)])]
    with-return))

(defn funcall? [col]
  (or (= 'fn (first col))
      (list? col)
      (seq? col)))

(defn convert-map-access-function [[keyword map]]
  (let [keyword (convert-el keyword)
        map (convert-el map)]
    (str map "['" keyword "']")))

(defn convert-apply-function [[_ f & args]]
  (str
   "(function(){"
   "})"))

(declare js-call)
(declare js-apply)

(defn convert-list [l]
  (let [f (first l)]
    (cond
     (= 'apply f) (js-apply l)
     (symbol? f) (js-call l)
     (keyword? f) (convert-map-access-function l)
     (string? f) (map convert-el l)
     (funcall? f) (js-call l))))

(declare convert-symbol)
(defn convert-symbol-with-ns [s]
  (let [[ns var] (-> (str s)
                     (str/split #"\/" 2))]
    (symbol (str (str/replace ns #"\." "_DOT_") "." (convert-symbol var)))))

(declare strict-handlers)

(defn convert-symbol [s]
  (cond ((strict-handlers) s) (((strict-handlers) s))
        :else (if (re-find #"\/" (str s))
                (convert-symbol-with-ns s)
                (-> (str s)
                    (str/replace #"-" "_")
                    (str/replace #"\?" "_QM_")
                    (symbol)))))

(defn convert-map [m]
  (str
   "(function(){"
   "return {"
   (apply str (interpose "," (map #(str \' (name (key %)) \' ":" (convert-el (val %))) m)))
   "}"
   ";})()"))

(defn convert-string [s]
  (str \" s \"))

(defn convert-number [n]
  (str n))

(defn convert-vector [v]
  (str "[" (apply str (interpose "," (map convert-el v))) "]"))

(defn emit-function [arglist body-seq]
  (let [body-seq (map convert-el body-seq)
        body-len (dec (count body-seq))
        before-ret (take body-len body-seq)
        after-ret (drop body-len body-seq)
        with-return (concat before-ret [(apply str "return " after-ret)])
        before-amp (take-while #(not= '& %) arglist)
        after-amp (first (drop 1 (drop-while #(not= '& %) arglist)))]
    (str "(function("
         (apply str (interpose "," (map convert-el before-amp)))
         "){\n"
         (when after-amp
           (str "var " (convert-el after-amp)
                " = Array.prototype.slice.call(arguments," (count before-amp) ");"))
         (apply str (interpose ";\n" with-return))
         ";\n})")))

(defn chop-trailing-period [sym]
  (let [sym-str (str sym)
        len (count sym-str)]
    (->> sym-str
         (take (dec len))
         (apply str)
         (symbol))))



(defn convert-el [el]
  (cond
   (or (list? el) (seq? el)) (convert-list el)
   (string? el) (convert-string el)
   (symbol? el) (convert-symbol el)
   (map? el) (convert-map el)
   (vector? el) (convert-vector el)
   (number? el) (convert-number el)
   (= java.lang.Boolean (class el)) (if el 'true 'false)
   (keyword? el) (name el)))


;; # Specific Function Handlers
;;
;; When **cljs** encounters a symbol in the first position of
;; a list, it assumes you're making a function call.
;; Usually **cljs** will do a straight translation:
;;
;;     (js '(foo "bar" "baz"))
;;     -> foo("bar","baz")
;;
;; In certain situations, we'd like to override this behavior,
;; as in the case of `+`.
;;
;;     Wrong:
;;     (js '(+ 1 1))
;;     -> +(1,1)
;;
;;     Right:
;;     (js '(+ 1 1))
;;     -> (1 + 1)
;;
;; `strict-handlers` and `lazy-handlers` (see the end of this section) provide
;; the mechanism to support this behavoir by mapping the desired symbol (i.e. `+`)
;; to a handler (i.e. `handle-+`).

(defn handle-println []
  "console.log")

(defn handle-fn [col]
  (let [_ (first col)
        arglist (second col)
        body (rest (drop 1 col))]
    (emit-function arglist body)))

(defn handle-binding [[v binding]]
  (str "var " (convert-el v) " = " (convert-el binding) ";\n"))

(defn handle-let [[_ bindings & body]]
  (str
   "(function(){"
   (apply str (map handle-binding (partition 2 bindings)))
   (apply str (interpose ";" (add-return (map convert-el body))))
   "})()"))

(defn handle-doto [[_ & body]]
  (let [pivot (first body)
        forms (rest body)]
    (str
     "(function(){"
     (apply
      str
      (interpose
       ";\n"
       (add-return
         (concat
          [(str "var out = " (convert-el pivot))]
          (map convert-el (map #(concat (vector (first %) 'out) (rest %)) forms))
          ['out]))))
     "}())")))

(defn handle-->> [[_ pivot & forms]]
  (let [pivot (convert-el pivot)
        forms (map #(concat % ['out])
                   forms)]
    (str
     "(function(){"
     "var out = "
     pivot
     ";\n"
     (apply str (map #(str "out = " % ";") (map convert-el forms)))
     "return out;"
     "})()")))

(defn handle-def [[_ v body]]
  (str "var " (convert-el v) " = " (convert-el body) ";\n"
       (cns-with-dot) (convert-el v) " = " (convert-el v)))

(defn handle-defn [col]
  (let [[_ name & fndef] col]
    (str "var " (convert-el name) " = " (emit-function (first fndef) (rest fndef))
         ";\n"
         (cns-with-dot) (convert-el name) " = " (convert-el name) ";")))

(defn call-fn [f args]
  (let [f (convert-el f)
        args (map convert-el args)]
    (str "("
         f
         "("
         (apply str (interpose "," args))
         "))")))

(defn reduce-args [op init]
  (str
   "(function(){"
   "return _.reduce(arguments,function(c,v){return c " op " v}," init ")"
   "})"))

(defn str-fn []
  (reduce-args '+ "\"\""))

(defn call-dot-fn [col]
  (let [f (convert-el (first col))
        obj (convert-el (second col))
        args (drop 2 col)]
    (call-fn (symbol (str obj f)) args)))

(defn call-new-fn [col]
  (let [clazz  (chop-trailing-period (first col))
        args (rest col)]
    (str "(new "
         clazz
         "("
         (apply str (interpose "," (map convert-el args)))
         "))")))


(declare lazy-eval-fn-defs)
(declare strict-eval-fn-defs)

(defn js-call [fn]
  (let [[f & args] fn]
    (cond
     (lazy-eval-fn-defs f) ((lazy-eval-fn-defs f) fn)
     ((strict-handlers) f) (call-fn (first fn) (rest fn))
     (= \. (first (str f))) (call-dot-fn fn)
     (re-find #"\.$" (str f)) (call-new-fn fn)
     (symbol? f) (call-fn (first fn) (rest fn))
     :else (call-fn (first fn) (rest fn)))))

(defn js-apply-fn [f args]
  (let [l (last args)
        before-l (take (dec (count args)) args)]
    (str "("
         f
         ".apply(null,"
         (when (not (empty? before-l))
           (str "[" (apply str (interpose "," (map convert-el before-l))) "].concat("))
         (convert-el l)
         (when (not (empty? before-l))
           (str ")"))
         "))")))

(defn js-apply [fn]
  (let [[_ f & args] fn]
    (when (lazy-eval-fn-defs f)
      (throw (Exception. (str "Can't apply lazily evaluated fn: " f))))
    (if ((strict-handlers) f)
      (js-apply-fn (((strict-handlers) f)) args)
      (js-apply-fn f args))))


(defn handle-+ [& _]
  (reduce-args '+ 0))

(defn handle-= [[_ pivot & others]]
  (let [pivot (convert-el pivot)]
    (str
     \(
     (apply str (interpose " && " (map #(str pivot " == " %) (map convert-el others))))
     \))))

(defn handle-< [[_ pivot & others]]
  (let [pivot (convert-el pivot)]
    (str
     \(
     (apply str (interpose " && " (map #(str pivot " < " %) (map convert-el others))))
     \))))

(defn handle-> [[_ pivot & others]]
  (let [pivot (convert-el pivot)]
    (str
     \(
     (apply str (interpose " && " (map #(str pivot " > " %) (map convert-el others))))
     \))))

(defn handle-if [[_ pred t f]]
  (let [pred (convert-el pred)
        t (convert-el t)
        f (convert-el f)]
    (str
     "(function(){"
     "if(" pred "){\n return " t ";\n}"
     (when f
       (str "else{\n return " f ";\n}"))
     "})()")))

(defn handle-map [[_ f col]]
  (str
   "(function(){"
   "return _.map("
   (convert-el col)
   \,
   (convert-el f)
   ")"
   ";})()"))

(defn handle-filter [[_ f col]]
  (str
   "_.filter("
   (convert-el col)
   \,
   (convert-el f)
   ")"))

(defn handle-cond [[_ & conds]]
  (let [pairs (partition 2 conds)]
    (str
     "(function(){"
     (->> pairs
          (map #(str
                 (when (not (keyword? (first %)))
                   (str "if("
                        (convert-el (first %))
                        ")"))
                 "{"
                 "return "
                 (convert-el (second %))
                 ";"
                 "}"))
          (interpose " else ")
          (apply str))
     "})();")))

(defn handle-do [[_ & statements]]
  (str
   "(function(){"
   (apply str
          (interpose ";" (add-return (map convert-el statements))))
   "})()"))

(defn handle-first [[_ arr]]
  (str (convert-el arr) "[0]"))

(defn handle-ns [[_ ns & args]]
  (set-current-ns ns)
  (str
   "var " (cns) " = " (cns) " || {};"))

(defn handle-quote [[_ arg]]
  arg)

(defn handle-not [[_ stmt]]
  (str "(!" (convert-el stmt) ")"))

(defn handle-and [[_ & stmts]]
  (str
   "("
   (apply str (interpose " && " (map convert-el stmts)))
   ")"))

(defn handle-reduce [[_ f init col]]
  (when (not init)
    (throw (Exception. "No collection provided to reduce over.")))
  (str
   "(function(){"
   (if (not col)
     (str "var col = " (convert-el init) ";\n"
          "var memo = col[0];\n"
          "col = col.slice(1);\n")
     (str "var col = " (convert-el col) ";\n"
          "var memo = " (convert-el init) ";\n"))
   "return _.reduce("
   "col"
   \,
   (convert-el f)
   \,
   "memo"
   ");"
   "})()"))

(defn handle-merge []
  (str
   "(function(){"
   "var pivot = arguments[0];"
   "var rest = Array.prototype.slice.call(arguments,1);"
   "_.each(rest, function(el) {"
   "_.each(el, function(v,k) {"
   "pivot[k] = v;"
   "});"
   "});"
   "return pivot;"
   "})"))

#_'(apply merge [{:hello "world"} {:foo "bar"}])



(defn handle-assoc [[_ m & args]]
  (let [pairs (partition 2 args)]
    (str
     "(function(){"
     "var out = " (convert-el m) ";"
     (apply str (map #(str "out['" (convert-el (first %)) "'] = " (convert-el (second %)) ";") pairs))
     "return out;"
     "})()")))


(def lazy-eval-fn-defs
  {'fn      handle-fn
   'let     handle-let
   'doto    handle-doto
   'def     handle-def
   'defn    handle-defn
   '=       handle-=
   '>       handle->
   '<       handle-<
   'if      handle-if
   'map     handle-map
   'reduce  handle-reduce
   'cond    handle-cond
   'do      handle-do
   'first   handle-first
   'ns      handle-ns
   'quote   handle-quote
   'filter  handle-filter
   'not     handle-not
   'and     handle-and
   '->>     handle-->>
   'assoc   handle-assoc
})

(defn strict-handlers []
  {'str     str-fn
   '+       handle-+
   'println handle-println
   'merge   handle-merge})


;;
;;
;;

(defn js [form] (str (convert-el form) ";"))

(defn compile-cljs [path]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.FileReader. path))
        forms (take-while #(not (nil? %)) (repeatedly (fn [] (read rdr false nil))))
        first-form (first forms)
        ns-decl (when (= 'ns (first first-form))
                  first-form)
        forms (if ns-decl (rest forms) forms)]
    (when ns-decl)
         (set-current-ns (second ns-decl))
    (str "var " (cns) " = " (cns) " || {};\n"
         "(function() {\n"
         (apply str (interpose "\n" (map js forms)))
         "})();")))

(defn compile-to [cljs-path output-path]
  (spit output-path (compile-cljs cljs-path)))

(defn stich-cljs-output [js-path]
  (->> (java.io.File. js-path)
       (file-seq)
       (filter #(.endsWith (.getName %) ".cljs.js"))
       #_(map slurp)
       #_(apply str)))

