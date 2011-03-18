(ns cljs.core
  "**cljs**"
  (:require [clojure.string :as str]))

;; Overall, cljs tries to mimic Clojure's rules where possible.

;; ## Identifiers
;; Cljs allows you to include several characters which are not allowed
;; in javascript identifiers.

(defn prep-symbol
  "Prep-symbol will replace invalid characters in the passed symbol with
   javascript-compatable replacements.

       foo-bar  ->  foo_bar
       foo?     ->  foo_QM_
       foo#     ->  foo_HASH_
       foo!     ->  foo_BANG_
       foo/bar  ->  foo.bar
       foo*     ->  foo_SPLAT_"
  [s]
  (-> (str s)
      (str/replace #"-" "_")
      (str/replace #"\?" "_QM_")
      (str/replace #"#" "_HASH_")
      (str/replace #"!" "_BANG_")
      (str/replace #"/" ".")
      (str/replace #"\*" "_SPLAT_")
      (str/replace #"=" "_EQ_")
      (symbol)))

;; ### Indenting / Pretty-Printing
;; As of now, pretty printing js is the only option.  If you need to
;; minify the compiled output, use a tool like [Google's
;; Closure Compiler](http://code.google.com/closure/compiler/) (which
;; may be included in cljs in the future.
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

(defn returnable? [el]
  (cond
   (coll? el) (let [f (first el)]
                (not (or (= 'throw f))))
   (string? el) (not (re-find #"^throw\s+" el))
   :else true))

(defn add-return [statements]
  (let [count (dec (count statements))
        before-ret (take count statements)
        after-ret (first (drop count statements))
        with-return (concat before-ret [(str "return " after-ret)])]
    (if (returnable? after-ret)
      with-return
      statements)))

(defn interpose-semi-colon [col]
  (interpose ";" col))

;; Several types of functions follow the pattern `<expr> <op>
;; <expr>`. We use the *-op-fn functions to provide a uniform
;; mechanism for defining these types of operations.
;;
;; See section 1.2 of Okasaki's [Purely Functional Data
;; Structures](http://www.cs.cmu.edu/~rwh/theses/okasaki.pdf) for more
;; info. In fact, just go read the whole thing, it's amazingly
;; well-written.


(defn strict-eval-op-fn
  "`strict-eval-op-fn` is used to define functions of the above pattern for fuctions such as `+`, `*`,   etc.  Cljs special forms defined this way are applyable, such as `(apply + [1 2 3])`.

   Resulting expressions are wrapped in an anonymous function and, down the line, `call`ed, like so:

       (+ 1 2 3) -> (function(){...}.call(this, 1 2 3)"
  [op]
  (ind-str
   "(function() {" nl
   (inc-ind-str
    "var _out = arguments[0];" nl
    "for(var _i=1; _i<arguments.length; _i++) {" nl
    (inc-ind-str
     "_out = _out " op " arguments[_i];")
    nl
    "}" nl
    "return _out;")
   nl
   "})"))

(defn lazy-eval-op-fn
  "`lazy-eval-op-fn` is used for operators whose inputs must be lazily evaluated. Resulting expression   is not `apply`able.

       (or foo bar baz)  -> (foo || bar || baz)"
  [op stmts]
  (str
   "("
   (apply str (interpose (str " " op " ") (map to-js stmts)))
   ")"))


(def *fn-params* #{})

(defn to-identifier [sym]
  (when sym
    (prep-symbol sym)))


;; # Calling Functions

(defn call-fn [[f & args]]
  (ind-str
   (to-js f)
   "("
   (->> args
        (map to-js)
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
      (apply str (interpose (str ";" nl nl) (add-return (map to-js body)))))
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

(defn handle-while [[_ pred & body]]
  (ind-str
   "while("
   (to-js pred)
   ") {"
   (inc-ind-str
    (apply str (interpose (str ";" nl) (map to-js body))))
   nl
   "}"))

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
    (binding [*fn-params* (concat *fn-params* ['_out])]
      (str
       "(function(){"
       (apply
        str
        (interpose
         ";\n"
         (add-return
          (concat
           [(str "var _out = " (to-js pivot))]
           (map to-js (map #(concat (vector (first %) '_out) (rest %)) forms))
           ['_out]))))
       "}.bind(this))()"))))

(defn handle-->> [[_ pivot & forms]]
  (let [pivot (to-js pivot)
        forms (map #(concat % ['_out])
                   forms)]
    (binding [*fn-params* (concat *fn-params* ['_out])]
      (str
       "(function(){"
       "var _out = "
       pivot
       ";\n"
       (apply str (map #(str "_out = " % ";" nl) (map to-js forms)))
       "return _out;"
       "}.bind(this))()"))))

(defn handle--> [[_ pivot & forms]]
  (let [pivot (to-js pivot)
        forms (map #(concat [(first %)] [''_out] (rest %))
                   forms)]
    (binding [*fn-params* (concat *fn-params* ['_out])]
      (ind-str
       "(function(){"
       (inc-ind-str
        "var _out = "
        pivot
        ";\n"
        (apply str (map #(str "_out = " % ";" nl) (map to-js forms)))
        "return _out;")
       "}.bind(this))()"))))

(defn handle-not [[_ stmt]]
  (str "(!" (to-js stmt) ")"))

(defn handle-do [[_ & statements]]
  (str
   "(function(){"
   (apply str
          (interpose (str ";" nl) (add-return (map to-js statements))))
   "}.bind(this))()"))

(defn handle-cond [[_ & conds]]
  (let [pairs (partition 2 conds)]
    (ind-str
     "(function(){" nl
     (inc-ind-str
      (->> pairs
           (map #(str
                  (when (not (keyword? (first %)))
                    (str "if("
                         (to-js (first %))
                         ")"))
                  "{" nl
                  (inc-ind-str
                   (if (returnable? (second %))
                     (str "return "
                          (to-js (second %)))
                     (to-js (second %)))
                   ";") nl
                  "}"))
           (interpose " else ")
           (apply str)))
     "}.bind(this))()")))

(defn make-strict-op [op]
  (fn [& _]
    (strict-eval-op-fn op)))

(defn make-lazy-op [op]
  (fn [[_ & args]]
    (lazy-eval-op-fn op args)))

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

(defn handle-gensym [_]
  (to-identifier (gensym)))

(defn handle-gensym-str [_]
  (to-js (str (gensym))))

(defn handle-throw [[_ msg]]
  (ind-str
   "throw " (to-js msg) ";"))

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
   'while   handle-while
   'when    handle-when
   'doto    handle-doto
   '->      handle-->
   '->>     handle-->>
   'not     handle-not
   'do      handle-do
   'cond    handle-cond
   '=       (make-lazy-op '==)
   '>       (make-lazy-op '>)
   '<       (make-lazy-op '<)
   '>=      (make-lazy-op '>=)
   '<=      (make-lazy-op '<=)
   'or      (make-lazy-op '||)
   'and     (make-lazy-op '&&)
   'doseq   handle-doseq
   'instanceof handle-instanceof
   'gensym handle-gensym
   'gensym-str handle-gensym-str
   'throw   handle-throw})

(defn apply-able-special-forms []
  {'+       (make-strict-op '+)
   '-       (make-strict-op '-)
   '*       (make-strict-op '*)
   '/       (make-strict-op '/)})

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

(defn sexp-to-js
  "Sexps are (most of the time) translated into javascript function calls.
   The baseline translation works as you would expect, the first form in
   the sexp is moved outside the opening paren, and the remaining forms
   are passed as arguments:

       (dostuff \"foo\" \"bar\" 1  -> alert(\"foo\",\"bar\",1)

   However, cljs supports special case sexps to support features like
   sugared map access and javascript object interop.

        ; object member function
        (.foo bar \"baz\") -> foo.bar(\"baz\")

        ; object construction
        (Foo. \"bar\" \"baz\") -> new Foo(\"bar\",\"baz\")

        ; map access
        (:foo bar) -> bar[\"foo\"]

   It is also at this point that calls to special forms are handled by
   their respective handlers. `(fn [] ('alert \"hi\"))` is
   handled by `handle-fn`, not passed along to `call-fn` like user
   defined functions.

   See `cljs.core/special-forms` and `cljs.core/apply-able-special-forms`
   for symbol to handler mappings."
  [sexp]
  (cond
   (= 'quote (first sexp)) (str (second sexp))
   (object-member? sexp) (object-member-call-to-js sexp)
   (new-object? sexp) (new-object sexp)
   ((special-forms) (first sexp)) (((special-forms) (first sexp)) sexp)
   ((apply-able-special-forms) (first sexp)) (call-special-form sexp)
   (map-accessor? sexp) (map-accessor-to-js sexp)
   :else (call-fn sexp)))

;; hashmap creation is handled in a seemingly convoluted way. A
;; temporary object is created to which entries are inserted, instead
;; of a direct translation to javascript's map syntax.  This is to
;; support using var values as keys.  For example:
;;
;;     var x = "foo"
;;     var m = {x: "bar"}
;;
;; would produce an object with a member "x" of value "bar", where the
;; desired behavoir is a map with member "foo" of value
;; "bar". Therefore we handle map creation as such:
;;
;;     (let [x "foo"]
;;       {x "bar"})
;;
;;       compiles to
;;
;;     var x = "foo"
;;     return (function() {
;;       var _out = {}
;;       _out[x] = "bar"
;;     })()
;;
;;       instead of
;;
;;     var x = "foo"
;;     {x: "bar"}
;;
;; which more closely mimics clojure's map creation behavior.
;;

(defn map-to-js [m]
  (ind-str
   "(function(){" nl
   (inc-ind-str
    "var _out = {};" nl
    (->> m
         (map #(str
                "_out[" (to-js (key %)) "] = " (to-js (val %)) ";" nl))
         (apply str))
    "return _out;")
   nl
   "}.bind(this))()"))

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

(defn str-to-js [s]
  (-> s
      (str/replace #"\n" "\\\\n")
      (str/replace #"\"" "\\\\\"")))

(defn to-js
  "Top-level conversion routine. The form passed to `to-js` is converted to javascript based on it's type.  Valid input types are lists, vectors, symbols, keywords, strings, numbers, or booleans. Throws an exeption if the passed form is not a valid input type."
  [form]
  (cond
   (or (seq? form)
       (list? form))  (sexp-to-js form)
   (map? form)        (map-to-js form)
   (vector? form)     (vec-to-js form)
   (symbol? form)     (symbol-to-js form)
   (keyword? form)    (to-js (name form))
   (string? form)     (str \" (str-to-js form) \")
   (number? form)     form
   (boolean? form)    form
   (nil? form)        ""
   :else              (throw
                       (Exception.
                        (str
                         "Don't know how to handle "
                         form
                         " of type "
                         (:type form))))))


;; ## Namespace Handling

(defn use-to-js [u]
  (->> u
       (drop 1)
       (map #(str
              "for("
              "var prop in " (to-identifier %)
              ")"
              "{ this[prop] = " (to-identifier %) "[prop] };" nl nl))
       (apply str)))

(defn seq-require-to-js
  "Handles `:require`s in the form of `[foo :as bar]`."
  [[name _ as]]
  (str "this." (to-identifier as) " = " (to-identifier name) ";" nl nl))

(defn sym-require-to-js [sym]
  (str "this." (to-identifier sym) " = " (to-identifier sym) ";" nl nl))

(defn require-to-js [r]
  (->> r
       (drop 1)
       (map #(cond
              (or (vector? %) (seq? %)) (seq-require-to-js %)
              :else (sym-require-to-js %)))
       (apply str)))

(defn import-to-js [r]
  (require-to-js r))

(defn init-ns-object [name]
  (when name
    (let [parts (str/split (str name) #"\.")
          num (count parts)]
      (->> (map #(->> parts
                      (take (inc %))
                      (interpose ".")
                      (apply str))
                (range num))
           (map #(str
                  (when (not (re-find #"\." %))
                    "var ")
                  (to-identifier %)
                  " = "
                  (to-identifier %)
                  " || {};"
                  nl))))))


(def default-includes ['Array])

(defn wrap-with-ns [name imports & body]
  (ind-str
   (apply str (init-ns-object name))
   "(function() {" nl nl
   (inc-ind-str
    (apply str (interpose ";\n" (map #(str "this." % " = " %) default-includes)))
    ";\n\n"
    (use-to-js '(:use cljs.core))
    (->> imports
         (filter #(= :use (first %)))
         (map use-to-js)
         (interpose (str ";" nl nl))
         (apply str))
    (->> imports
         (filter #(= :require (first %)))
         (map require-to-js)
         (interpose (str ";" nl nl))
         (apply str))
    (->> imports
         (filter #(= :import (first %)))
         (map import-to-js)
         (interpose (str ";" nl nl))
         (apply str))
    (->> body
         (map to-js)
         (add-return)
         (interpose (str ";" nl nl))
         (apply str)))
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

     '(defn apply [f & args]
        (let [l (last args)
              fs (take (dec (count args)) args)
              flattened (concat fs l)]
          (.apply f 'this flattened)))

     '(defn filter [f col]
        (if col
          (.filter _ col f)))

     '(defn concat [cola colb]
        (let [out []]
          (out.push.apply out cola)
          (out.push.apply out colb)
          out))

     '(defn take [n col]
        (when col
          (.slice col 0 n)))

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

     '(defn interpose [o col]
        (when col
          (let [out []
                idx 0
                len (count col)
                declen (dec len)]
            (while (< idx len)
              (if (= idx declen)
                (.push out (aget col idx))
                (do
                  (.push out (aget col idx))
                  (.push out o)))
              (set! idx (inc idx)))
            out)))

     '(defn interleave [cola colb]
        (if (or (= 0 (count cola))
                (= 0 (count colb)))
          []
          (let [len (if (> (count cola) (count colb))
                      (count cola)
                      (count colb))]
            (concat [(first cola) (first colb)]
                    (interleave (rest cola) (rest colb))))))

     '(defn distinct [col]
        (.uniq '_ col))

     '(defn identity [arg]
        (if arg
          (.identity '_ arg)))

     '(defn empty? [col]
        (cond
         (array? col) (= 0 col.length)
         (object? col) (.isEqual '_ {} col)
         :else (throw (str "Can't call empty? on " col))))

     '(defn hash-map [& col]
        (let [pairs (partition 2 col)]
          (if (empty? col)
            {}
            (reduce (fn [m pair]
                      (aset m (first pair) (second pair))
                      m)
                    {}
                    pairs)))))))

(defn spit-cljs-core [path]
  (spit path *core-lib*))

(defn compile-cljs-reader [reader]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. reader)
        forms (take-while #(not (nil? %)) (repeatedly (fn [] (read rdr false nil))))
        ns-decl (when (= 'ns (first (first forms)))
                  (first forms))
        forms (if ns-decl (rest forms) forms)
        imports (drop 2 ns-decl)]
    (apply wrap-with-ns
           (second ns-decl)
           imports
           forms)))

(defn compile-cljs-string [str]
  (compile-cljs-reader (java.io.StringReader. str)))

(defn compile-cljs-file [path]
  (compile-cljs-reader (java.io.FileReader. path)))


