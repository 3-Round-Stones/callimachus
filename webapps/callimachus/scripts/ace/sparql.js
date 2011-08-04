define("ace/mode/sparql", function(require, exports, module) {

var oop = require("pilot/oop");
var TextMode = require("ace/mode/text").Mode;
var Tokenizer = require("ace/tokenizer").Tokenizer;
var SparqlHighlightRules = require("ace/mode/sparql_highlight_rules").SparqlHighlightRules;
var MatchingBraceOutdent = require("ace/mode/matching_brace_outdent").MatchingBraceOutdent;
var Range = require("ace/range").Range;

var Mode = function() {
  this.$tokenizer = new Tokenizer(new SparqlHighlightRules().getRules());
};
oop.inherits(Mode, TextMode);

exports.Mode = Mode;

});
