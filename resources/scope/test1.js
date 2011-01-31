foo = "bar";

var makeScope = function(outerScope) {
    return _.clone((outerScope || (function(){return this;}).call().scope));
}

scope = _.clone((function(){return this;}).call());

scope.a = 1;


scope.x = function() {
    var scope = makeScope(scope);

    scope.stuff = -1;

    scope.b = function() {
        var scope = makeScope(scope);

        return scope.stuff;
    }

    return scope.b()
}



var hello = {};

(function() {
    //imports
    var global = (function(){return this;}).call();

    this.a = 5;
    this.b = 1;

    this.add = function() {
       this.bar = function() {
            return this.a + this.b;
       }

       return this.bar()
    }

    this.add()

    this.asdf = this.bar()

    this.underscore = global._

}).call(hello);

var world = {};
(function() {
    var global = (function(){return this;}).call();
    //use
    for (var prop in global.hello) {this[prop] = global.hello[prop]};
}).call(world)


var foo = {};
(function() {
    var global = (function(){return this;}).call();
    //require
    this.hello = global.hello
}).call(foo)


foo.hello.b
