define("ace/mode/sparql_highlight_rules", function(require, exports, module) {

var oop = require("pilot/oop");
var lang = require("pilot/lang");
var TextHighlightRules = require("ace/mode/text_highlight_rules").TextHighlightRules;

var SparqlHighlightRules = function() {

  var builtinFunctions = lang.arrayToMap(
    "str|lang|langmatches|datatype|bound|sameterm|isiri|isuri|isblank|isliteral|union|a".split("|")
  );

  var keywords = lang.arrayToMap(
    ("base|BASE|prefix|PREFIX|select|SELECT|ask|ASK|construct|CONSTRUCT|describe|DESCRIBE|where|WHERE|"+
     "from|FROM|reduced|REDUCED|named|NAMED|order|ORDER|limit|LIMIT|offset|OFFSET|filter|FILTER|"+
     "optional|OPTIONAL|graph|GRAPH|by|BY|asc|ASC|desc|DESC").split("|")
  );

  var buildinConstants = lang.arrayToMap(
    "true|TRUE|false|FALSE".split("|")
  );

  var builtinVariables = lang.arrayToMap(
    ("").split("|")
  );

  // regexp must not have capturing parentheses. Use (?:) instead.
  // regexps are ordered -> the first match is used

  this.$rules = {
    "start" : [
      {
        token : "comment",
        regex : "#.*$"
      }, {
        token : "sparql.iri.constant.buildin",
        regex : "\\<\\S+\\>"
      }, {
        token : "sparql.variable",
        regex : "[\\?\\$][a-zA-Z]+"
      }, {
        token : "sparql.prefix.constant.language",
        regex : "[a-zA-Z]+\\:"
      }, {
        token : "string.regexp",
        regex : "[/](?:(?:\\[(?:\\\\]|[^\\]])+\\])|(?:\\\\/|[^\\]/]))*[/]\\w*\\s*(?=[).,;]|$)"
      }, {
        token : "string", // single line
        regex : '["](?:(?:\\\\.)|(?:[^"\\\\]))*?["]'
      }, {
        token : "string", // single line
        regex : "['](?:(?:\\\\.)|(?:[^'\\\\]))*?[']"
      }, {
        token : "constant.numeric", // hex
        regex : "0[xX][0-9a-fA-F]+\\b"
      }, {
        token : "constant.numeric", // float
        regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
      }, {
        token : "constant.language.boolean",
        regex : "(?:true|false)\\b"
      }, {
        token : function(value) {
          if (value == "self")
            return "variable.language";
          else if (keywords.hasOwnProperty(value))
            return "keyword";
          else if (buildinConstants.hasOwnProperty(value))
            return "constant.language";
          else if (builtinVariables.hasOwnProperty(value))
            return "variable.language";
          else if (builtinFunctions.hasOwnProperty(value))
            return "support.function";
          else if (value == "debugger")
            return "invalid.deprecated";
          else
            return "identifier";
        },
        regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
      }, {
        token : "keyword.operator",
        regex : "\\*|\\+|\\|\\-|\\<|\\>|=|&|\\|"
      }, {
        token : "lparen",
        regex : "[\\<({]"
      }, {
        token : "rparen",
        regex : "[\\>)}]"
      }, {
        token : "text",
        regex : "\\s+"
      }
    ],
    "comment" : [
      {
        token : "comment", // comment spanning whole line
        regex : ".+"
      }
    ]
  };
};

oop.inherits(SparqlHighlightRules, TextHighlightRules);
exports.SparqlHighlightRules = SparqlHighlightRules;
});
