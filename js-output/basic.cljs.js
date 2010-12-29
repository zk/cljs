var basic = basic || {};
(function() {
var greet = function(x,y){
return console.log((x+y));
};
var basic.greet = greet;
greet("hello","world");
var x = 5;
var basic.x = x;})();