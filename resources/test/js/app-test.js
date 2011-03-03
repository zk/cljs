if(!Function.prototype.bind){Function.prototype.bind = function(scope) {var _function = this;return function() { return _function.apply(scope, arguments); } }}var cljs = cljs || {};
cljs.core = cljs.core || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  this._ = _;
  
  this.count = (function(col){
    return (function(){
      if(col){
       return col.length;
      } else {
       return 0;
      }
    }.bind(this))();
  }.bind(this));
  
  this.first = (function(col){
    return (function(){
      
      if(!col) return null;
      
      return (col[0]);
    
    }.bind(this))();
  }.bind(this));
  
  this.second = (function(col){
    return this.nth(col, 1);
  }.bind(this));
  
  this.rest = (function(col){
    return (function(){
      
      if(!col) return null;
      
      return this.Array.prototype.slice["call"](col,1);
    
    }.bind(this))();
  }.bind(this));
  
  this.inc = (function(n){
    return (function() {
      var _out = arguments[0];
      for(var _i=1; _i<arguments.length; _i++) {
        _out = _out + arguments[_i];
      }
      return _out;
    }).call(this, n, 1);
  }.bind(this));
  
  this.dec = (function(n){
    return (function() {
      var _out = arguments[0];
      for(var _i=1; _i<arguments.length; _i++) {
        _out = _out - arguments[_i];
      }
      return _out;
    }).call(this, n, 1);
  }.bind(this));
  
  this.nth = (function(col, n){
    return (function(){
      
      if(!(col && (col.length > n))) return null;
      
      return (col[n]);
    
    }.bind(this))();
  }.bind(this));
  
  this.last = (function(col){
    return (col[this.dec(col.length)]);
  }.bind(this));
  
  this.reduce = (function(f, initial, col){
    return (function(){
      var i = (function(){
        if(col){
         return initial;
        } else {
         return null;
        }
      }.bind(this))(),
      c = (function(){
        if(col){
         return col;
        } else {
         return initial;
        }
      }.bind(this))();
      
      return (function(){
        if(i){
         return this._["reduce"](c,f,i);
        } else {
         return this._["reduce"](c,f);
        }
      }.bind(this))();
    
    }.bind(this))();
  }.bind(this));
  
  this.map = (function(f, initial, col){
    return (function(){
      var i = (function(){
        if(col){
         return initial;
        } else {
         return null;
        }
      }.bind(this))(),
      c = (function(){
        if(col){
         return col;
        } else {
         return initial;
        }
      }.bind(this))();
      
      return (function(){
        
        if(!c) return null;
        
        return (function(){
          if(i){
           return this._["map"](c,f,i);
          } else {
           return this._["map"](c,f);
          }
        }.bind(this))();
      
      }.bind(this))();
    
    }.bind(this))();
  }.bind(this));
  
  this.str = (function(){
    var args = Array.prototype.slice.call(arguments, 0);
    return this.reduce((function(col, el){
      return (function() {
        var _out = arguments[0];
        for(var _i=1; _i<arguments.length; _i++) {
          _out = _out + arguments[_i];
        }
        return _out;
      }).call(this, col, el);
    }.bind(this)), "", this.filter((function(p1__1785_HASH_){
      return this._["identity"](p1__1785_HASH_);
    }.bind(this)), args));
  }.bind(this));
  
  this.println = (function(){
    var args = Array.prototype.slice.call(arguments, 0);
    return console["log"](args);
  }.bind(this));
  
  this.apply = (function(f, args){
    return f["apply"](this,args);
  }.bind(this));
  
  this.filter = (function(f, col){
    return (function(){
      if(col){
       return this._["filter"](col,f);
      }
    }.bind(this))();
  }.bind(this));
  
  this.concat = (function(cola, colb){
    return (function(){
      var out = [];
      
      out.push.apply(out, cola);
      
      out.push.apply(out, colb);
      
      return out;
    
    }.bind(this))();
  }.bind(this));
  
  this.take = (function(n, col){
    return col["slice"](0,n);
  }.bind(this));
  
  this.drop = (function(n, col){
    return (function(){
      
      if(!col) return null;
      
      return col["slice"](n);
    
    }.bind(this))();
  }.bind(this));
  
  this.partition = (function(n, col){
    return (function(){
      var f = (function(out, col){
        return (function(){
          if((0 == this.count(col))){
           return out;
          } else {
           return f(this.concat(out, [
            this.take(n, col)
          ]), this.drop(n, col));
          }
        }.bind(this))();
      }.bind(this));
      
      return f([], col);
    
    }.bind(this))();
  }.bind(this));
  
  this.assoc = (function(obj){
    var rest = Array.prototype.slice.call(arguments, 1);
    return (function(){
      var pairs = this.partition(2, rest);
      
      (function() {
        var G__1787 = pairs;
        for(var i=0; i < G__1787.length; i++) {
          (function(p){(obj[this.first(p)] = this.nth(p, 1))}.bind(this))(G__1787[i]);
        }
      }.bind(this))();
      
      return obj;
    
    }.bind(this))();
  }.bind(this));
  
  this.conj = (function(col){
    var rest = Array.prototype.slice.call(arguments, 1);
    (function() {
      var G__1788 = rest;
      for(var i=0; i < G__1788.length; i++) {
        (function(r){col["push"](r)}.bind(this))(G__1788[i]);
      }
    }.bind(this))();
    return col;
  }.bind(this));
  
  this.array_QM_ = (function(o){
    return (o && this._["isArray"](o));
  }.bind(this));
  
  this.object_QM_ = (function(o){
    return (o && (!this.array_QM_(o)) && (!this.string_QM_(o)));
  }.bind(this));
  
  this.string_QM_ = (function(o){
    return this._["isString"](o);
  }.bind(this));
  
  this.element_QM_ = (function(o){
    return (o && (this._["isElement"](o) || this._["isElement"](this.first(o))));
  }.bind(this));
  
  this.merge = (function(){
    var objs = Array.prototype.slice.call(arguments, 0);
    return (function(){
      var o = ({
        
      });
      
      this.map((function(p1__1786_HASH_){
        return this._["extend"](o,p1__1786_HASH_);
      }.bind(this)), objs);
      
      return o;
    
    }.bind(this))();
  }.bind(this));
  
  this.interpose = (function(o, col){
    return (function(){
      
      if(!col) return null;
      
      return (function(){
        var out = [],
        idx = 0,
        len = this.count(col),
        declen = this.dec(len);
        
        while((idx < len)) {  (function(){
            if((idx == declen)){
             return out["push"]((col[idx]));
            } else {
             return (function(){out["push"]((col[idx]));
            return out["push"](o)}.bind(this))();
            }
          }.bind(this))();
          (idx = this.inc(idx))
        };
        
        return out;
      
      }.bind(this))();
    
    }.bind(this))();
  }.bind(this));
  
  this.distinct = (function(col){
    return _["uniq"](col);
  }.bind(this));
  
  return this.identity = (function(arg){
    return (function(){
      if(arg){
       return _["identity"](arg);
      }
    }.bind(this))();
  }.bind(this))

}).call(cljs.core);



var util = util || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  this.$ = jQuery;
  
  this.RegExp = RegExp;
  
  this.has_el_QM_ = (function(o){
    return (function(){
      
      if(!o) return null;
      
      return (o["el"]);
    
    }.bind(this))();
  }.bind(this));
  
  this.append = (function(p, c){
    (function(){
      if(this.array_QM_(c)){
        return this.map((function(c){
          return this.append(p, c);
        }.bind(this)), c);
      } else if(this.has_el_QM_(c)){
        return this.append(p, (c['el']));
      } else {
        return (function(){p["append"](c);
        return (function(){
          
          if(!(c instanceof jQuery)) return null;
          
          return c["trigger"]("postinsert");
        
        }.bind(this))()}.bind(this))();
      }}.bind(this))();
    return p;
  }.bind(this));
  
  this.replace_in = (function(p, c){
    p["empty"]();
    return this.append(p, c);
  }.bind(this));
  
  this.take = (function(n, o){
    return (function(){
      if(this.string_QM_(o)){
        return o["substring"](0,n);
      }}.bind(this))();
  }.bind(this));
  
  this.h1_QM_ = (function(el){
    return (el.type == "h1");
  }.bind(this));
  
  this.paragraph_QM_ = (function(el){
    return (el.type == "paragraph");
  }.bind(this));
  
  this.run_QM_ = (function(el){
    return ((!(null == el)) && (el.type == "run"));
  }.bind(this));
  
  this.ordered_list_QM_ = (function(el){
    return ((!(null == el)) && (el.type == "ordered-list"));
  }.bind(this));
  
  this.run_content = (function(el){
    return (function(){
      if(el){
       return el.content;
      }
    }.bind(this))();
  }.bind(this));
  
  this.text_content = (function(el){
    return (function(){
      if(this.h1_QM_(el)){
        return this.apply(this.str, this.map(this.run_content, el.content));
      }}.bind(this))();
  }.bind(this));
  
  this.apply_str = (function(ss){
    return this.reduce((function(col, s){
      return (function() {
        var _out = arguments[0];
        for(var _i=1; _i<arguments.length; _i++) {
          _out = _out + arguments[_i];
        }
        return _out;
      }).call(this, col, s);
    }.bind(this)), ss);
  }.bind(this));
  
  this.ellipsis = (function(s, n){
    return (function(){
      if((s.length > n)){
       return this.str(s["substring"](0,n), "...");
      } else {
       return s;
      }
    }.bind(this))();
  }.bind(this));
  
  this.loading_indicator = this.$("#loading-indicator");;
  
  this.loading = (function(enable){
    return (function(){
      if(enable){
       return this.loading_indicator["css"](({
        'display':"block"
      }));
      } else {
       return this.loading_indicator["hide"](({
        'display':"none"
      }));
      }
    }.bind(this))();
  }.bind(this));
  
  this.$(document)["ready"]((function(){
    return (this.loading_indicator = this.$("#loading-indicator"));
  }.bind(this)));
  
  this.make_url_friendly = (function(s){
    return (function(){  var _out = s;
      _out = _out["replace"]((new this.RegExp("-","g")),"_");
      _out = _out["replace"]((new this.RegExp(" ","g")),"_");
      _out = _out["toLowerCase"]();
      return _out;}.bind(this))();
  }.bind(this));
  
  this.ready = (function(f){
    return this.$(document)["ready"](f);
  }.bind(this));
  
  this.has_layout_QM_ = (function(o){
    return (function(){
      
      if(!o) return null;
      
      return (o["layout"]);
    
    }.bind(this))();
  }.bind(this));
  
  return this.ajax = (function(opts){
    return this.$["ajax"](opts);
  }.bind(this))

}).call(util);



var html = html || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  for(var prop in util){ this[prop] = util[prop] };
  
  this.$ = jQuery;
  
  this.parse_attrs = (function(args){
    return (function(){
      if((this.nth(args, 1) instanceof jQuery)){
        return ({
          
        });
      } else if(this.object_QM_(this.nth(args, 1))){
        return this.nth(args, 1);
      } else {
        return ({
          
        });
      }}.bind(this))();
  }.bind(this));
  
  this.parse_body = (function(args){
    return (function(){var _out = (function(){
      if((this.nth(args, 1) instanceof jQuery)){
        return this.drop(1, args);
      } else if(this.object_QM_(this.nth(args, 1))){
        return this.drop(2, args);
      } else {
        return this.drop(1, args);
      }}.bind(this))();
    _out = this.filter(this._.identity, _out);
    _out = this.filter((function(p1__2611_HASH_){
      return (!(undefined == p1__2611_HASH_));
    }.bind(this)), _out);
    return _out;}.bind(this))();
  }.bind(this));
  
  this.html = (function(args){
    return (function(){
      if(this.string_QM_(args)){
        return args;
      } else if(this.element_QM_(args)){
        return args;
      } else if(this.element_QM_(this.first(args))){
        return args;
      } else if((args instanceof jQuery)){
        return args;
      } else if(this.array_QM_(this.first(args))){
        return this.map(this.html, args);
      } else if(this.has_el_QM_(args)){
        return (args['el']);
      } else if(true){
        return (function(){
          var as = this.filter(this._.identity, args),
          tag = this.first(as),
          attrs = this.parse_attrs(as),
          body = this.parse_body(as),
          el = this.$(this.str("<", tag, "/>"));
          
          (function(){
            if(attrs){
             return el["attr"](attrs);
            }
          }.bind(this))();
          
          return this.append(el, this.map(this.html, body));
        
        }.bind(this))();
      }}.bind(this))();
  }.bind(this));
  
  return this.$html = (function(args){
    return this.$(this.html(args));
  }.bind(this))

}).call(html);



var templates = templates || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  for(var prop in html){ this[prop] = html[prop] };
  
  this.header = (function(){
    return this.$html([
      "header",
      [
        "h1",
        "Cljs"
      ],
      [
        "p",
        "An experimental Clojure(ish)-to-Javascript compiler."
      ]
    ]);
  }.bind(this));
  
  this.why = (function(){
    return this.section("Why?", "why", [
      "ul",
      [
        "li",
        "Learn more about clojure."
      ],
      [
        "li",
        "Find out what makes a lisp a lisp."
      ],
      [
        "li",
        "I was tired of writing javascript.  Love the language, meh on the syntax."
      ],
      [
        "li",
        "I was having trouble keeping things modular as SLOC grew."
      ]
    ], [
      "p",
      "At the time of this writing there are several other clojure-to-javascript compilers, including clojurescript, clojurejs, and scriptjure. I didn't go with one of these because I needed the freedom to explore the problem, make mistakes, without mucking up other peoples projcets."
    ]);
  }.bind(this));
  
  this.section = (function(title, class){
    var content = Array.prototype.slice.call(arguments, 2);
    return this.$html([
      "div",
      ({
        'class':this.str("section ", class)
      }),
      [
        "h2",
        title
      ],
      content
    ]);
  }.bind(this));
  
  this.features = (function(){
    return this.section("Features", "features", [
      "ul",
      [
        "li",
        "Namespaces"
      ],
      [
        "li",
        "Continuous Compilation"
      ],
      [
        "li",
        "Dependency Management"
      ]
    ]);
  }.bind(this));
  
  return this.missing = (function(){
    return this.section("Missing", "missing", [
      "ul",
      [
        "li",
        "Macros"
      ]
    ]);
  }.bind(this))

}).call(templates);



var app = app || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  for(var prop in html){ this[prop] = html[prop] };
  
  this.u = util;
  
  this.tpl = templates;
  
  this.$ = jQuery;
  
  this.RE = RegExp;
  
  this.keyup_timer = (function(input, on_timeout){
    return (function(){
      var jqi = this.$(input),
      delay = 300,
      last_text = input["val"](),
      timer = setTimeout((function(){
        return ;
      }.bind(this)), delay),
      handler = (function(){
        return (function(){
          
          if(!(!(last_text == jqi["val"]()))) return null;
          
          on_timeout(jqi["val"](), last_text);
          return (last_text = jqi["val"]());
        
        }.bind(this))();
      }.bind(this)),
      reset = (function(){
        return (function(){clearTimeout(timer);
        return (timer = setTimeout(handler, delay))}.bind(this))();
      }.bind(this));
      
      input["keyup"]((function(){
        return reset();
      }.bind(this)));
      
      return jqi;
    
    }.bind(this))();
  }.bind(this));
  
  this.code = (function(code_str){
    return (function(){
      var ta = this.$html([
        "textarea",
        ({
          'class':"cljs-input"
        }),
        code_str
      ]),
      output = this.$html([
        "pre",
        ({
          'class':"cljs-output"
        })
      ]),
      error_indicator = this.$html([
        "div",
        ({
          'class':"error-indicator"
        }),
        "!"
      ]),
      with_compiled = (function(new_js){
        output["html"](new_js);
        (last_js = new_js);
        return error_indicator["css"](({
          'display':"none"
        }));
      }.bind(this)),
      last_js = "";
      
      ta["focus"]((function(){
        return output["fadeIn"]();
      }.bind(this)));
      
      this.compile(code_str, with_compiled);
      
      return this.$html([
        "div",
        ({
          'class':"code-area"
        }),
        this.keyup_timer(ta, (function(n, o){
          return this.compile(n, with_compiled, (function(){
            return error_indicator["css"](({
              'display':"block"
            }));
          }.bind(this)));
        }.bind(this))),
        output,
        error_indicator
      ]);
    
    }.bind(this))();
  }.bind(this));
  
  this.compile = (function(code_str, with_compiled, on_error){
    return this.ajax(({
      'url':"/compile",
      'data':({
        'code':code_str
      }),
      'type':"POST",
      'success':(function(resp){
        return with_compiled(resp);
      }.bind(this)),
      'error':on_error
    }));
  }.bind(this));
  
  this.code1 = this.code("(println \"hello world\")\n(println \"bar\")");;
  
  this.ready((function(){
    return (function(){
      var body = this.$("body");
      
      return this.u.append(body, this.$html([
        "div",
        this.tpl.header(),
        this.tpl.why(),
        this.tpl.features(),
        this.tpl.missing(),
        this.code1
      ]));
    
    }.bind(this))();
  }.bind(this)));
  
  return this.diff = (function(n, o){
    return (function(){  var _out = diffString(o, n);
      _out = _out["replace"]((new this.RE(" ","g")),"&nbsp;");
      _out = _out["replace"]((new this.RE("\n","g")),"<br />");
      return _out;}.bind(this))();
  }.bind(this))

}).call(app);



var app_test = app_test || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  for(var prop in app){ this[prop] = app[prop] };
  
  this.TestCase = TestCase;
  
  this.is = (function(test){
    return assertTrue(test);
  }.bind(this));
  
  this.deftest = (function(name){
    var tests = Array.prototype.slice.call(arguments, 1);
    return (function(){
      var tc = this.TestCase(name);
      
      return this.map((function(p1__2612_HASH_){
        return ((tc.prototype[this.str("test-", "G__2613")]) = p1__2612_HASH_);
      }.bind(this)), tests);
    
    }.bind(this))();
  }.bind(this));
  
  return this.deftest("foo", (function(){
    return this.is((1 == 2));
  }.bind(this)))

}).call(app_test);