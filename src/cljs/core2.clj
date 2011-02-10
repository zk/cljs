(ns cljs.core2
  (:require [clojure.string :as str]))

(def *indent* 0)

(def nl "\n")

(declare to-js)
(declare special-forms)
(declare apply-able-special-forms)

(defn ind []
  (apply str (take *indent* (repeat " "))))

(defn ind-str [& args]
  (let [lines (-> (apply str args)
                  (str/split #"\n"))
        with-indent (interpose nl (map #(str (ind) %) lines))]
    (apply str with-indent)))

(defmacro with-inc-indent [& body]
  `(binding [*indent* (+ *indent* 2)]
     ~@body))

(defn inc-ind-str [& body]
  (with-inc-indent
    (apply ind-str body)))

(defn add-return [statements]
  (let [count (dec (count statements))
        before-ret (take count statements)
        after-ret (drop count statements)
        with-return (concat before-ret [(apply str "return " after-ret)])]
    with-return))

(defn interpose-semi-colon [col]
  (interpose ";" col))

(defn interpose-op-fn [op]
  (ind-str
   "(function() {" nl
   (inc-ind-str
    "var out = arguments[0];" nl
    "for(var __i=1; __i<arguments.length; __i++) {" nl
    (inc-ind-str
     "out = out " op " arguments[__i];")
    nl
    "}" nl
    "return out;")
   nl
   "})"))

(defn boolean-op-fn [op stmts]
  (str
   "("
   (apply str (interpose (str " " op " ") (map to-js stmts)))
   ")"))

(defn create-scope []
  (ind-str
   "var __scope = __mk_scope(__scope);" nl))

(def *mk-scope*
  (str "__mk_scope = (function(__scope){ var __res = {}; __res.__proto__ = __scope; return __res;});"))

(def *fn-params* #{})

(defn prep-symbol [s]
  (-> (str s)
      (str/replace #"-" "_")
      (str/replace #"\?" "_QM_")
      (str/replace #"#" "_HASH_")
      (str/replace #"!" "_BANG_")
      (str/replace #"/" ".")
      (symbol)))

(defn to-identifier [sym]
  (when sym
    (prep-symbol sym)))

(defn to-fn [[_ arglist & body-seq]]
  (let [before-amp (take-while #(not= '& %) arglist)
        after-amp (first (drop 1 (drop-while #(not= '& %) arglist)))
        params (concat before-amp [after-amp])
        before-amp (map to-identifier before-amp)
        after-amp (to-identifier after-amp)]
    (binding [*fn-params* (set (concat params *fn-params*))]
      (let [body-seq (map to-js body-seq)
            body-len (dec (count body-seq))
            before-ret (take body-len body-seq)
            after-ret (drop body-len body-seq)
            with-return (concat before-ret [(apply str "return " after-ret)])]
        (str "(function("
             (apply str (interpose ", " before-amp))
             "){\n"
             (inc-ind-str
              (when after-amp
                (str "var " after-amp
                     " = Array.prototype.slice.call(arguments, " (count before-amp) ");" nl))
              (apply str (interpose (str ";" nl) with-return)))
             ";\n}.bind(this))")))))


(defn call-fn [[f & args]]
  (ind-str
   (to-js f)
   "("
   (->> args
        (map to-js)
        (interpose ", ")
        (apply str))
   ")"))


(defn call-fn-using-call [[f & args]]
  (ind-str
   (to-js f)
   #_".call(this"
   "("
   (->> args
        (map to-js)
        #_(interleave (repeat ", "))
        (interpose ", ")
        (apply str))
   ")"))



(defn call-special-form [sexp]
  (let [f (first sexp)
        args (rest sexp)
        jsf (((apply-able-special-forms) f) sexp)]
    (ind-str
     jsf
        ".call(this"
   (->> args
        (map to-js)
        (interleave (repeat ", "))
        #_(interpose ", ")
        (apply str))
   ")")))


(defn js [form]
  (to-js form))

(defn handle-def [[_ name body]]
  (ind-str
   "this." (to-identifier name) " = " (to-js body) ";"))

(defn handle-fn [sexp]
  (to-fn sexp))

(defn handle-set [[_ name val]]
  (ind-str
   "("
   (to-js name)
   " = "
   (to-js val)
   ")"))

(defn handle-binding [[v binding]]
  (str "" (to-identifier v) " = " (to-js binding)))

(defn handle-bindings [col]
  (str
   "var "
   (->> (partition 2 col)
        (map handle-binding)
        (interpose (str "," nl))
        (apply str))
   ";"))

(defn binding-vars [bindings]
  (->> (partition 2 bindings)
      (map first)))

(defn handle-let [[_ bindings & body]]
  (binding [*fn-params* (concat *fn-params* (binding-vars bindings))]
    (ind-str
     "(function(){" nl
     (inc-ind-str
      (handle-bindings bindings) nl nl
      (apply str (interpose ";" (add-return (map to-js body)))))
     ";"
     nl nl
     "}.bind(this))()")))

(defn handle-defn [[_ & rest]]
  (let [name (first rest)
        fn (drop 1 rest)]
    (ind-str
     "this." (to-identifier name) " = "
     (to-fn rest)
     )))

(defn handle-aget [[_ col idx]]
  (ind-str
   "("
   (to-js col)
   "[" (to-js idx) "]"
   ")"))

(defn handle-aset [[_ col idx val]]
  (ind-str
   "("
   (to-js col)
   "[" (to-js idx) "]"
   " = "
   (to-js val)
   ")"))

(defn handle-if [[_ pred t f]]
  (let [pred (to-js pred)
        t (to-js t)
        f (to-js f)]
    (str
     "(function(){" nl
     (inc-ind-str
      "if(" pred "){\n return " t ";\n}"
      (when f
        (str " else {\n return " f ";\n}")))
     nl
     "}.bind(this))()")))

(defn handle-when [[_ pred & rest]]
  (let [pred (to-js pred)
        rest (add-return (map to-js rest))]
    (ind-str
     "(function(){" nl
     (inc-ind-str
      nl
      "if(!" pred ") return null;" nl nl
      (apply str (interpose (str ";" nl) rest))
      ";")
     nl nl
     "}.bind(this))()")))


(defn handle-doto [[_ & body]]
  (let [pivot (first body)
        forms (rest body)]
    (binding [*fn-params* (concat *fn-params* ['out])]
      (str
       "(function(){"
       (apply
        str
        (interpose
         ";\n"
         (add-return
          (concat
           [(str "var out = " (to-js pivot))]
           (map to-js (map #(concat (vector (first %) 'out) (rest %)) forms))
           ['out]))))
       "}.bind(this)())"))))

(defn handle-->> [[_ pivot & forms]]
  (let [pivot (to-js pivot)
        forms (map #(concat % ['out])
                   forms)]
    (binding [*fn-params* (concat *fn-params* ['out])]
      (str
       "(function(){"
       "var out = "
       pivot
       ";\n"
       (apply str (map #(str "out = " % ";") (map to-js forms)))
       "return out;"
       "}.bind(this))()"))))

(defn handle--> [[_ pivot & forms]]
  (let [pivot (to-js pivot)
        forms (map #(concat [(first %)] [''out] (rest %))
                   forms)]
    (binding [*fn-params* (concat *fn-params* ['out])]
      (str
       "(function(){"
       "var out = "
       pivot
       ";\n"
       (apply str (map #(str "out = " % ";") (map to-js forms)))
       "return out;"
       "}.bind(this))()"))))

(defn handle-not [[_ stmt]]
  (str "(!" (to-js stmt) ")"))

(defn handle-do [[_ & statements]]
  (str
   "(function(){"
   (apply str
          (interpose ";" (add-return (map to-js statements))))
   "}.bind(this))()"))

(defn handle-cond [[_ & conds]]
  (let [pairs (partition 2 conds)]
    (str
     "(function(){"
     (->> pairs
          (map #(str
                 (when (not (keyword? (first %)))
                   (str "if("
                        (to-js (first %))
                        ")"))
                 "{"
                 "return "
                 (to-js (second %))
                 ";"
                 "}"))
          (interpose " else ")
          (apply str))
     "}.bind(this))();")))

(defn make-handle-op [op]
  (fn [& _]
    (interpose-op-fn op)))

(defn make-boolean-op [op]
  (fn [[_ & args]]
    (boolean-op-fn op args)))

(defn handle-doseq [[_ bdg & body]]
  (let [colsym (gensym)]
    (ind-str
     "(function() {" nl
     (inc-ind-str
      "var " colsym " = " (to-js (second bdg)) ";" nl
      "for(var i=0; i < " colsym  ".length; i++) {" nl
      (inc-ind-str
       "(function(" (to-identifier (first bdg)) "){"
       (binding [*fn-params* (concat *fn-params* [(first bdg)])]
         (->> body
              (map to-js)
              (interpose (str ";" nl))
              (apply str))))
      "}.bind(this))(" colsym "[i]);"
      nl
      "}") nl
      "}.bind(this))()")))

(defn handle-instanceof [[_ obj type]]
  (ind-str
   "("
   (to-js obj)
   " instanceof "
   (to-js type)
   ")"))

(defn special-forms []
  {'def     handle-def
   'fn      handle-fn
   'fn*     handle-fn
   'set!    handle-set
   'let     handle-let
   'defn    handle-defn
   'aget    handle-aget
   'aset    handle-aset
   'if      handle-if
   'when    handle-when
   'doto    handle-doto
   '->      handle-->
   '->>     handle-->>
   'not     handle-not
   'do      handle-do
   'cond    handle-cond
   '=       (make-boolean-op '==)
   '>       (make-boolean-op '>)
   '<       (make-boolean-op '<)
   '>=      (make-boolean-op '>=)
   '<=      (make-boolean-op '<=)
   'or      (make-boolean-op '||)
   'and     (make-boolean-op '&&)
   'doseq   handle-doseq
   'instanceof handle-instanceof})

(defn apply-able-special-forms []
  {'+       (make-handle-op '+)
   '-       (make-handle-op '-)
   '*       (make-handle-op '*)
   '/       (make-handle-op '/)})

(defn map-accessor? [sexp]
  (and (= 2 (count sexp))
       (or (seq? sexp)
           (list? sexp))
       (keyword? (first sexp))))

(defn map-accessor-to-js [sexp]
  (let [kw (name (first sexp))
        obj (to-js (second sexp))]
    (str "(" obj "['" kw "'])")))

(defn object-member? [[f & _]]
  (= \. (first (str f))))

(defn object-member-call-to-js [[member obj & args]]
  (ind-str
   (to-js obj)
   "[\""
   (str/replace member #"\." "")
   "\"]"
   "("
   (->> args
        (map to-js)
        (interpose ",")
        (apply str))
   ")"))

(defn chop-trailing-period [sym]
  (let [sym-str (str sym)
        len (count sym-str)]
    (->> sym-str
         (take (dec len))
         (apply str)
         (symbol))))

(defn new-object? [[obj & _]]
  (re-find #"\.$" (str obj)))

(defn new-object [[obj & args]]
  (let [clazz (chop-trailing-period obj)]
    (str "(new "
         "this."
         clazz
         "("
         (apply str (interpose "," (map to-js args)))
         "))")))

(defn sexp-to-js [sexp]
  (cond
   (= 'quote (first sexp)) (str (second sexp))
   (object-member? sexp) (object-member-call-to-js sexp)
   (new-object? sexp) (new-object sexp)
   ((special-forms) (first sexp)) (((special-forms) (first sexp)) sexp)
   ((apply-able-special-forms) (first sexp)) (call-special-form sexp)
   (map-accessor? sexp) (map-accessor-to-js sexp)
   :else (call-fn-using-call sexp)))

(defn map-to-js [m]
  (ind-str
   "({" nl
   (inc-ind-str
    (apply str (interpose (str "," nl (ind)) (map #(str \' (name (key %)) \' ":" (to-js (val %))) m))))
   nl "})"))

(defn vec-to-js [elements]
  (if (empty? elements) "[]"
      (ind-str
       "[" nl
       (inc-ind-str
        (->> elements
             (map to-js)
             (interpose ",\n")
             (apply str)))
       nl "]")))

(defn scope-symbol [sym]
  (if (seq? sym)
    sym
    (let [before-dot (symbol (second (re-find #"^([^.]*)" (str sym))))
          sym (prep-symbol sym)]
      (if (some #(= before-dot %) *fn-params*)
        sym
        (str "this." sym)))))

(defn symbol-to-js [sym]
  (cond
   ((apply-able-special-forms) sym) (((apply-able-special-forms) sym))
   :else (scope-symbol sym)))

(defn boolean? [o]
  (= java.lang.Boolean (type true)))

(defn to-js [element]
  (cond
   (or (seq? element) (list? element)) (sexp-to-js element)
   (map? element) (map-to-js element)
   (vector? element) (vec-to-js element)
   (symbol? element) (symbol-to-js element)
   (keyword? element) (to-js (name element))
   (string? element) (str \" element \")
   (number? element) element
   (boolean? element) element
   (nil? element) ""
   :else (throw (Exception. (str "Don't know how to handle " element " of type " (:type element))))))

(def default-includes ['Array])

(defn use-to-js [u]
  (->> u
       (drop 1)
       (map #(str "for("
                      "var prop in " (to-identifier %)
                      ")"
                      "{ this[prop] = " (to-identifier %) "[prop] };" nl nl))
       (apply str)))

(defn seq-require-to-js [col]
  (let [name (first col)
        as (nth col 2)]
    (str
     "this." (to-identifier as) " = " (to-identifier name) ";" nl nl)))

(defn sym-require-to-js [sym]
  (str
   "this." (to-identifier sym) " = " (to-identifier sym) ";" nl nl))

(defn require-to-js [r]
  (->> r
       (drop 1)
       (map #(cond
              (or (vector? %) (seq? %)) (seq-require-to-js %)
              :else (sym-require-to-js %)))
       (apply str)))

(defn init-ns-object [name]
  (let [parts (str/split (str name) #"\.")
        num (count parts)]
    (->> (map #(->> parts
                    (take (inc %))
                    (interpose ".")
                    (apply str))
              (range num))
         (map #(str (when (not (re-find #"\." %)) "var ") (to-identifier %) " = " (to-identifier %) " || {};" nl)))))


(defn wrap-with-ns [name imports & body]
  (ind-str
   (apply str (init-ns-object name))
   "(function() {" nl nl
   (inc-ind-str
    (apply str (interpose ";\n" (map #(str "this." % " = " %) default-includes)))
    ";\n\n"
    (use-to-js '(:use cljs.core))
    (apply str (interpose ";\n\n" (map use-to-js (filter #(= :use (first %)) imports))))
    (apply str (interpose ";\n\n" (map require-to-js (filter #(= :require (first %)) imports))))
    (apply str (interpose (str ";" nl nl) (add-return (map to-js body)))))
   nl nl
   "}).call(" (to-identifier name) ");"))

(def *core-lib*
  (str
   "if(!Function.prototype.bind){"
   "Function.prototype.bind = function(scope) {var _function = this;return function() { return _function.apply(scope, arguments); } }"
   "}"
   (wrap-with-ns "cljs.core" '[(:require _)]
    
     '(defn count [col]
        (if col
          'col.length
          0))

     '(defn first [col]
        (when col
          (aget col 0)))

     '(defn second [col]
        (nth col 1))

     '(defn rest [col]
        (when col
             (.call Array.prototype.slice col 1)))

     '(defn inc [n]
        (+ n 1))

     '(defn dec [n]
        (- n 1))

     '(defn nth [col n]
        (when (and col (> col.length n))
          (aget col n)))

     '(defn last [col]
        (aget col (dec col.length)))

     '(defn reduce [f initial col]
        (let [i (if col initial 'null)
              c (if col col initial)]
          (if i
            (.reduce _ c f i)
            (.reduce _ c f))))

     '(defn map [f initial col]
        (let [i (if col initial 'null)
              c (if col col initial)]
          (when c
            (if i
              (.map _ c f i)
              (.map _ c f)))))

     '(defn str [& args]
        (reduce (fn [col el] (+ col el)) "" (filter #(.identity _ %) args)))

     '(defn println [& args]
        (.log 'console args))

     '(defn apply [f args]
        (.apply f 'this args))

     '(defn filter [f col]
        (if col
          (.filter _ col f)))

     '(defn concat [cola colb]
        (let [out []]
          (out.push.apply out cola)
          (out.push.apply out colb)
          out))

     '(defn take [n col]
        (.slice col 0 n))

     '(defn drop [n col]
        (when col
          (.slice col n)))

     '(defn partition [n col]
        (let [f (fn [out col]
                  (if (= 0 (count col))
                    out
                    (f (concat out [(take n col)]) (drop n col))))]
          (f [] col)))

     '(defn assoc [obj & rest]
        (let [pairs (partition 2 rest)]
          (doseq [p pairs]
            (aset obj (first p) (nth p 1)))
          obj))

     '(defn conj [col & rest]
        (doseq [r rest]
          (.push col r))
        col)

     '(defn array? [o]
        (and o
             (.isArray _ o)))

     '(defn object? [o]
        (and o
             (not (array? o))
             (not (string? o))))

     '(defn string? [o]
        (.isString _ o))

     '(defn element? [o]
        (and o
             (or (.isElement _ o)
                 (.isElement _ (first o)))))

     '(defn merge [& objs]
        (let [o {}]
          (map #(.extend _ o %) objs)
          o))

     )))

(defn spit-cljs-core [path]
  (spit path *core-lib*))

(defn compile-cljs-file [path]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.FileReader. path))
        forms (take-while #(not (nil? %)) (repeatedly (fn [] (read rdr false nil))))
        ns-decl (when (= 'ns (first (first forms)))
                  (first forms))
        forms (if ns-decl (rest forms) forms)
        imports (drop 2 ns-decl)]
    (apply wrap-with-ns
           (second ns-decl)
           imports
           forms)))


