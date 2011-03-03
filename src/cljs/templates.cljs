(ns templates
  (:use html))

(defn header []
  ($html
   [:header
    [:h1 "Cljs"]
    [:p "An experimental Clojure(ish)-to-Javascript compiler."]]))

(defn why []
  (section
   "Why?"
   :why
   [:ul
    [:li "Learn more about clojure."]
    [:li "Find out what makes a lisp a lisp."]
    [:li "I was tired of writing javascript.  Love the language, meh on the syntax."]
    [:li "I was having trouble keeping things modular as SLOC grew."]]
   [:p "At the time of this writing there are several other clojure-to-javascript compilers, including clojurescript, clojurejs, and scriptjure. I didn't go with one of these because I needed the freedom to explore the problem, make mistakes, without mucking up other peoples projcets."]))

(defn section [title class & content]
  ($html
   [:div {:class (str "section " class)}
    [:h2 title]
    content]))

(defn features []
  (section
   "Features"
   :features
   [:ul
    [:li "Namespaces"]
    [:li "Continuous Compilation"]
    [:li "Dependency Management"]]))

(defn missing []
  (section
   "Missing"
   :missing
   [:ul
    [:li "Macros"]]))





