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
    }.bind(this)), "", this.filter((function(p1__1950_HASH_){
      return this._["identity"](p1__1950_HASH_);
    }.bind(this)), args));
  }.bind(this));
  
  this.println = (function(){
    var args = Array.prototype.slice.call(arguments, 0);
    return console["log"](args);
  }.bind(this));
  
  this.apply = (function(f){
    var args = Array.prototype.slice.call(arguments, 1);
    return (function(){
      var l = this.last(args),
      fs = this.take(this.dec(this.count(args)), args),
      flattened = this.concat(fs, l);
      
      return f["apply"](this,flattened);
    
    }.bind(this))();
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
    return (function(){
      
      if(!col) return null;
      
      return col["slice"](0,n);
    
    }.bind(this))();
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
        var G__1952 = pairs;
        for(var i=0; i < G__1952.length; i++) {
          (function(p){(obj[this.first(p)] = this.nth(p, 1))}.bind(this))(G__1952[i]);
        }
      }.bind(this))();
      
      return obj;
    
    }.bind(this))();
  }.bind(this));
  
  this.conj = (function(col){
    var rest = Array.prototype.slice.call(arguments, 1);
    (function() {
      var G__1953 = rest;
      for(var i=0; i < G__1953.length; i++) {
        (function(r){col["push"](r)}.bind(this))(G__1953[i]);
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
      var o = (function(){
        var _out = {};
        return _out;
      }.bind(this))();
      
      this.map((function(p1__1951_HASH_){
        return this._["extend"](o,p1__1951_HASH_);
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
  
  this.interleave = (function(cola, colb){
    return (function(){
      if(((0 == this.count(cola)) || (0 == this.count(colb)))){
       return [];
      } else {
       return (function(){
        var len = (function(){
          if((this.count(cola) > this.count(colb))){
           return this.count(cola);
          } else {
           return this.count(colb);
          }
        }.bind(this))();
        
        return this.concat([
          this.first(cola),
          this.first(colb)
        ], this.interleave(this.rest(cola), this.rest(colb)));
      
      }.bind(this))();
      }
    }.bind(this))();
  }.bind(this));
  
  this.distinct = (function(col){
    return _["uniq"](col);
  }.bind(this));
  
  this.identity = (function(arg){
    return (function(){
      if(arg){
       return _["identity"](arg);
      }
    }.bind(this))();
  }.bind(this));
  
  this.empty_QM_ = (function(col){
    return (function(){
      if(this.array_QM_(col)){
        return (0 == col.length);
      } else if(this.object_QM_(col)){
        return _["isEqual"]((function(){
          var _out = {};
          return _out;
        }.bind(this))(),col);
      } else {
        throw this.str("Can't call empty? on ", col);;
      }}.bind(this))();
  }.bind(this));
  
  return this.hash_map = (function(){
    var col = Array.prototype.slice.call(arguments, 0);
    return (function(){
      var pairs = this.partition(2, col);
      
      return (function(){
        if(this.empty_QM_(col)){
         return (function(){
          var _out = {};
          return _out;
        }.bind(this))();
        } else {
         return this.reduce((function(m, pair){
          (m[this.first(pair)] = this.second(pair));
          return m;
        }.bind(this)), (function(){
          var _out = {};
          return _out;
        }.bind(this))(), pairs);
        }
      }.bind(this))();
    
    }.bind(this))();
  }.bind(this))

}).call(cljs.core);



var sub = sub || {};
sub.one = sub.one || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  return this.one_fn = (function(){
    return this.println("one");
  }.bind(this))

}).call(sub.one);



var sub = sub || {};
sub.two = sub.two || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  return this.foo = "bar";

}).call(sub.two);



var app = app || {};
(function() {

  this.Array = Array;
  
  for(var prop in cljs.core){ this[prop] = cljs.core[prop] };
  
  for(var prop in sub.one){ this[prop] = sub.one[prop] };
  
  for(var prop in sub.two){ this[prop] = sub.two[prop] };
  
  return 

}).call(app);