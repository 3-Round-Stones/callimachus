define("ace/mode/sparql", function(require, exports, module) {

var oop = require("../lib/oop");
var TextMode = require("./text").Mode;
var Tokenizer = require("../tokenizer").Tokenizer;
var SparqlHighlightRules = require("./sparql_highlight_rules").SparqlHighlightRules;
var MatchingBraceOutdent = require("./matching_brace_outdent").MatchingBraceOutdent;
var Range = require("../range").Range;

var Mode = function() {
  this.$tokenizer = new Tokenizer(new SparqlHighlightRules().getRules());
};
oop.inherits(Mode, TextMode);

exports.Mode = Mode;

});
define("ace/mode/sparql_highlight_rules", function(require, exports, module) {

var oop = require("../lib/oop");
var lang = require("../lib/lang");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

var SparqlHighlightRules = function() {

  var builtinFunctions = lang.arrayToMap(
    "str|lang|langmatches|datatype|bound|sameterm|isiri|isuri|isblank|isliteral|union|a".split("|")
  );

  var keywords = lang.arrayToMap(['a'].concat([
        'ABS', 'ADD', 'ALL', 'AS', 'ASC', 'ASK', 'AVG', 'BASE', 'BIND',
        'BNODE', 'BOUND', 'BY', 'CEIL', 'CLEAR', 'COALESCE', 'CONCAT',
        'CONSTRUCT', 'CONTAINS', 'COPY', 'COUNT', 'CREATE',
        'DATATYPE', 'DAY', 'DEFAULT', 'DELETE', 'DESC', 'DESCRIBE',
        'DISTINCT', 'DROP', 'EXISTS', 'FILTER', 'FLOOR', 'FROM',
        'GRAPH', 'GROUP', 'HAVING', 'HOURS', 'IF', 'IN', 'INSERT',
        'INTO', 'IRI', 'isBLANK', 'isIRI', 'isLITERAL', 'isNUMERIC',
        'isURI', 'LANG', 'LANGMATCHES', 'LCASE', 'LIMIT', 'LOAD',
        'MAX', 'MD5', 'MIN', 'MINUS', 'MINUTES', 'MONTH', 'MOVE',
        'NAMED', 'NOT', 'NOW', 'OFFSET', 'OPTIONAL', 'ORDER',
        'PREFIX', 'RAND', 'REDUCED', 'REGEX', 'REPLACE', 'ROUND',
        'sameTerm', 'SAMPLE', 'SECONDS', 'SELECT', 'SEPARATOR',
        'SERVICE', 'SHA1', 'SHA256', 'SHA384', 'SHA512', 'SILENT',
        'STR', 'STRAFTER', 'STRBEFORE', 'STRDT', 'STRENDS', 'STRLANG',
        'STRLEN', 'STRSTARTS', 'STRUUID', 'SUBSTR', 'SUM', 'TIMEZONE',
        'TO', 'TZ', 'UCASE', 'UNDEF', 'UNION', 'URI', 'USING', 'UUID',
        'VALUES', 'WHERE', 'WITH', 'YEAR'
    ], [
        'abs', 'add', 'all', 'as', 'asc', 'ask', 'avg', 'base', 'bind',
        'bnode', 'bound', 'by', 'ceil', 'clear', 'coalesce', 'concat',
        'construct', 'contains', 'copy', 'count', 'create',
        'datatype', 'day', 'default', 'delete', 'desc', 'describe',
        'distinct', 'drop', 'exists', 'filter', 'floor', 'from',
        'graph', 'group', 'having', 'hours', 'if', 'in', 'insert',
        'into', 'iri', 'isblank', 'isiri', 'isliteral', 'isnumeric',
        'isuri', 'lang', 'langmatches', 'lcase', 'limit', 'load',
        'max', 'md5', 'min', 'minus', 'minutes', 'month', 'move',
        'named', 'not', 'now', 'offset', 'optional', 'order',
        'prefix', 'rand', 'reduced', 'regex', 'replace', 'round',
        'sameterm', 'sample', 'seconds', 'select', 'separator',
        'service', 'sha1', 'sha256', 'sha384', 'sha512', 'silent',
        'str', 'strafter', 'strbefore', 'strdt', 'strends', 'strlang',
        'strlen', 'strstarts', 'struuid', 'substr', 'sum', 'timezone',
        'to', 'tz', 'ucase', 'undef', 'union', 'uri', 'using', 'uuid',
        'values', 'where', 'with', 'year'
    ]));

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
