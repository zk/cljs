# cljs

An experimental Clojure(ish) to Javascript compiler.

Docs: [http://zkim.github.com/cljs](http://zkim.github.com/cljs)

Demo: [http://50.16.182.181:8080/](http://50.16.182.181:8080/) (Chrome
only for now)

## Usage

1. `lein new cljs-test`

2. Add a `:cljs` entry to your project.clj:


    (defproject...
      :cljs {:source-path "src/cljs"
             :source-output-path "resources/public/js"
             :source-libs [some-ns.app]
             :test-path "test/cljs"
             :test-output-path "resources/testjs"
             :test-libs [app-test]})


3. Make sure the paths you specify above exist.

4. Add `[cljs "0.2"]` and `[lein-cljs "0.2"]` to your `:dev-dependencies`.

5. Then `lein deps` and `lein cljs watch`.  This will start the watcher,
which will automatically recompile your cljs libs when cljs source
files change.

6. You now have the ability to use lisp full-stack (kind of), begin
global domination.


## Examples

See
[http://github.com/zkim/cljs-contrib](http://github.com/zkim/cljs-contrib),
specifically:

* `project.clj` for info on the `:cljs` opts map.

* Clone the repo and open `resources/border-layout.html` and
`resources/panel.html` in your browser.  This will give you an idea of
 how to use the compiled cljs output.

Be sure to include underscore.js in a script tag before including any
compiled cljs output.

## Todo

* Integrate Google's Closure Compiler for minifying
* Integrate js-test-driver for testing

## Missing Features

* REPL -- Browser stuff from https://github.com/ivan4th/swank-js &
  comm from swank-clojure

* Macros

* TCO -- Possible with CPS? http://eriwen.com/javascript/cps-tail-call-elimination/

## License

Copyright (C) 2010 Zachary Kim

Distributed under the Eclipse Public License, the same as Clojure.
