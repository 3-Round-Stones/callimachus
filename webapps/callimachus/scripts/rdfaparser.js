/**
 * RDFaParser.js - A callback-based RDFa Parser for HTML documents and nodes.
 * 
 * This class is a fork of Green Turtle by R. Alexander Milowski <alex@milowski.com>, Copyright (c) 2011-2012, https://code.google.com/p/green-turtle/
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

function RDFaParser() {
	
	this.typeURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	this.objectURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#object";
	this.XMLLiteralURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral"; 
	this.PlainLiteralURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral";
	
	this.blankCounter = 0;
	this.absURIRE = /[\w\_\-]+:\S+/;
	this.nameChar = '[-A-Z_a-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD\u10000-\uEFFFF\.0-9\u00B7\u0300-\u036F\u203F-\u2040]';
	this.nameStartChar = '[\u0041-\u005A\u0061-\u007A\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF\u0100-\u0131\u0134-\u013E\u0141-\u0148\u014A-\u017E\u0180-\u01C3\u01CD-\u01F0\u01F4-\u01F5\u01FA-\u0217\u0250-\u02A8\u02BB-\u02C1\u0386\u0388-\u038A\u038C\u038E-\u03A1\u03A3-\u03CE\u03D0-\u03D6\u03DA\u03DC\u03DE\u03E0\u03E2-\u03F3\u0401-\u040C\u040E-\u044F\u0451-\u045C\u045E-\u0481\u0490-\u04C4\u04C7-\u04C8\u04CB-\u04CC\u04D0-\u04EB\u04EE-\u04F5\u04F8-\u04F9\u0531-\u0556\u0559\u0561-\u0586\u05D0-\u05EA\u05F0-\u05F2\u0621-\u063A\u0641-\u064A\u0671-\u06B7\u06BA-\u06BE\u06C0-\u06CE\u06D0-\u06D3\u06D5\u06E5-\u06E6\u0905-\u0939\u093D\u0958-\u0961\u0985-\u098C\u098F-\u0990\u0993-\u09A8\u09AA-\u09B0\u09B2\u09B6-\u09B9\u09DC-\u09DD\u09DF-\u09E1\u09F0-\u09F1\u0A05-\u0A0A\u0A0F-\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32-\u0A33\u0A35-\u0A36\u0A38-\u0A39\u0A59-\u0A5C\u0A5E\u0A72-\u0A74\u0A85-\u0A8B\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0\u0AB2-\u0AB3\u0AB5-\u0AB9\u0ABD\u0AE0\u0B05-\u0B0C\u0B0F-\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32-\u0B33\u0B36-\u0B39\u0B3D\u0B5C-\u0B5D\u0B5F-\u0B61\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99-\u0B9A\u0B9C\u0B9E-\u0B9F\u0BA3-\u0BA4\u0BA8-\u0BAA\u0BAE-\u0BB5\u0BB7-\u0BB9\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C33\u0C35-\u0C39\u0C60-\u0C61\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CDE\u0CE0-\u0CE1\u0D05-\u0D0C\u0D0E-\u0D10\u0D12-\u0D28\u0D2A-\u0D39\u0D60-\u0D61\u0E01-\u0E2E\u0E30\u0E32-\u0E33\u0E40-\u0E45\u0E81-\u0E82\u0E84\u0E87-\u0E88\u0E8A\u0E8D\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3\u0EA5\u0EA7\u0EAA-\u0EAB\u0EAD-\u0EAE\u0EB0\u0EB2-\u0EB3\u0EBD\u0EC0-\u0EC4\u0F40-\u0F47\u0F49-\u0F69\u10A0-\u10C5\u10D0-\u10F6\u1100\u1102-\u1103\u1105-\u1107\u1109\u110B-\u110C\u110E-\u1112\u113C\u113E\u1140\u114C\u114E\u1150\u1154-\u1155\u1159\u115F-\u1161\u1163\u1165\u1167\u1169\u116D-\u116E\u1172-\u1173\u1175\u119E\u11A8\u11AB\u11AE-\u11AF\u11B7-\u11B8\u11BA\u11BC-\u11C2\u11EB\u11F0\u11F9\u1E00-\u1E9B\u1EA0-\u1EF9\u1F00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC\u2126\u212A-\u212B\u212E\u2180-\u2182\u3041-\u3094\u30A1-\u30FA\u3105-\u312C\uAC00-\uD7A3\u4E00-\u9FA5\u3007\u3021-\u3029_]';
	this.NCNAME = new RegExp('^' + this.nameStartChar + this.nameChar + '*$');
	
	this.prefixes = {};
	this.defaultPrefixes = {
		"": "http://www.w3.org/1999/xhtml/vocab#",
		// w3c
		"grddl": "http://www.w3.org/2003/g/data-view#",
		"ma": "http://www.w3.org/ns/ma-ont#",
		"owl": "http://www.w3.org/2002/07/owl#",
		"rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
		"rdfa": "http://www.w3.org/ns/rdfa#",
		"rdfs": "http://www.w3.org/2000/01/rdf-schema#",
		"rif": "http://www.w3.org/2007/rif#",
		"skos": "http://www.w3.org/2004/02/skos/core#",
		"skosxl": "http://www.w3.org/2008/05/skos-xl#",
		"wdr": "http://www.w3.org/2007/05/powder#",
		"void": "http://rdfs.org/ns/void#",
		"wdrs": "http://www.w3.org/2007/05/powder-s#",
		"xhv": "http://www.w3.org/1999/xhtml/vocab#",
		"xml": "http://www.w3.org/XML/1998/namespace",
		"xsd": "http://www.w3.org/2001/XMLSchema#",
		// non-rec w3c
		"sd": "http://www.w3.org/ns/sparql-service-description#",
		"org": "http://www.w3.org/ns/org#",
		"gldp": "http://www.w3.org/ns/people#",
		"cnt": "http://www.w3.org/2008/content#",
		"dcat": "http://www.w3.org/ns/dcat#",
		"earl": "http://www.w3.org/ns/earl#",
		"ht": "http://www.w3.org/2006/http#",
		"ptr": "http://www.w3.org/2009/pointers#",
		// widely used (for some interpretation of wide ;)
		"cc": "http://creativecommons.org/ns#",
		"ctag": "http://commontag.org/ns#",
		"dc": "http://purl.org/dc/terms/",
		"dcterms": "http://purl.org/dc/terms/",
		"foaf": "http://xmlns.com/foaf/0.1/",
		"gr": "http://purl.org/goodrelations/v1#",
		"ical": "http://www.w3.org/2002/12/cal/icaltzd#",
		"og": "http://ogp.me/ns#",
		"rev": "http://purl.org/stuff/rev#",
		"sioc": "http://rdfs.org/sioc/ns#",
		"v": "http://rdf.data-vocabulary.org/#",
		"vcard": "http://www.w3.org/2006/vcard/ns#",
		"schema": "http://schema.org/"
	};
	

	this.parseURI = function(uri) {
		var SCHEME = new RegExp("^[A-Za-z][A-Za-z0-9\+\-\.]*\:");
		var match = SCHEME.exec(uri);
		if (!match) {
			throw "Bad URI value, no scheme: '" + uri + "'";
		}
		var parsed = { spec: uri };
		parsed.scheme = match[0].substring(0,match[0].length-1);
		parsed.schemeSpecificPart = parsed.spec.substring(match[0].length);
		if (parsed.schemeSpecificPart.charAt(0)=='/' && parsed.schemeSpecificPart.charAt(1)=='/') {
			this.parseGeneric(parsed);
		} else {
			parsed.isGeneric = false;
		}
		
		parsed.normalize = function() {
			if (!this.isGeneric) {
				return;
			}
			if (this.segments.length==0) {
				return;
			}
			// edge case of ending in "/."
			if (this.path.length>1 && this.path.substring(this.path.length-2)=="/.") {
				this.path = this.path.substring(0,this.path.length-1);
				this.segments.splice(this.segments.length-1,1);
				this.schemeSpecificPart = "//"+this.authority+this.path;
				if (typeof this.query != "undefined") {
					this.schemeSpecificPart += "?" + this.query;
				}
				if (typeof this.fragment != "undefined") {
					this.schemeSpecificPart += "#" + this.fragment;
				}
				this.spec = this.scheme+":"+this.schemeSpecificPart;
				return;
			}
			var end = this.path.charAt(this.path.length-1);
			if (end!="/") {
				end = "";
			}
			for (var i=0; i<this.segments.length; i++) {
				if (i>0 && this.segments[i]=="..") {
					this.segments.splice(i-1,2);
					i -= 2;
				}
				if (this.segments[i]==".") {
					this.segments.splice(i,1);
					i--;
				}
			}
			this.path = this.segments.length==0 ? "/" : "/"+this.segments.join("/")+end;
			this.schemeSpecificPart = "//"+this.authority+this.path;
			if (typeof this.query != "undefined") {
				this.schemeSpecificPart += "?" + this.query;
			}
			if (typeof this.fragment != "undefined") {
				this.schemeSpecificPart += "#" + this.fragment;
			}
			this.spec = this.scheme+":"+this.schemeSpecificPart;
		}
		
		parsed.resolve = function(href) {
			var 
				SCHEME = new RegExp("^[A-Za-z][A-Za-z0-9\+\-\.]*\:"),
				last
			;
			if (!href) {
				return this.spec;
			}
			if (href.charAt(0)=='#') {
				var lastHash = this.spec.lastIndexOf('#');
				return lastHash<0 ? this.spec+href : this.spec.substring(0,lastHash)+href;
			}
			if (!this.isGeneric) {
				throw "Cannot resolve uri against non-generic URI: "+this.spec;
			}
			var colon = href.indexOf(':');
			if (href.charAt(0)=='/') {
				return this.scheme+"://"+this.authority+href;
			} else if (href.charAt(0)=='.' && href.charAt(1)=='/') {
				if (this.path.charAt(this.path.length-1)=='/') {
					return this.scheme+"://"+this.authority+this.path+href.substring(2);
				} else {
					last = this.path.lastIndexOf('/');
					return this.scheme+"://"+this.authority+this.path.substring(0,last)+href.substring(1);
				}
			} else if (SCHEME.test(href)) {
				return href;
			} else if (href.charAt(0)=="?") {
				return this.scheme+"://"+this.authority+this.path+href;
			} else {
				if (this.path.charAt(this.path.length-1)=='/') {
					return this.scheme+"://"+this.authority+this.path+href;
				} else {
					last = this.path.lastIndexOf('/');
					return this.scheme+"://"+this.authority+this.path.substring(0,last+1)+href;
				}
			}
		};
	
		parsed.relativeTo = function(otherURI) {
			if (otherURI.scheme!=this.scheme) {
				return this.spec;
			}
			if (!this.isGeneric) {
				throw "A non generic URI cannot be made relative: "+this.spec;
			}
			if (!otherURI.isGeneric) {
				throw "Cannot make a relative URI against a non-generic URI: "+otherURI.spec;
			}
			if (otherURI.authority!=this.authority) {
				return this.spec;
			}
			var 
				i=0,
				j,
				relative
			;
			for (; i<this.segments.length && i<otherURI.segments.length; i++) {
				if (this.segments[i]!=otherURI.segments[i]) {
					//alert(this.path+" different from "+otherURI.path+" at '"+this.segments[i]+"' vs '"+otherURI.segments[i]+"'");
					relative = "";
					for (j=i; j<otherURI.segments.length; j++) {
						relative += "../";
					}
					for (j=i; j<this.segments.length; j++) {
						relative += this.segments[j];
						if ((j+1)<this.segments.length) {
							relative += "/";
						}
					}
					if (this.path.charAt(this.path.length-1)=='/') {
						relative += "/";
					}
					return relative;
				}
			}
			if (this.segments.length==otherURI.segments.length) {
				return this.hash ? this.hash : (this.query ? this.query : "");
			} else if (i<this.segments.length) {
				relative = "";
				for (j=i; j<this.segments.length; j++) {
					relative += this.segments[j];
					if ((j+1)<this.segments.length) {
						relative += "/";
					}
				}
				if (this.path.charAt(this.path.length-1)=='/') {
					relative += "/";
				}
				return relative;
			} else {
				throw "Cannot calculate a relative URI for "+this.spec+" against "+otherURI.spec;
			} 
		};
	
		return parsed;
	};

	this.parseGeneric = function(parsed) {
		if (parsed.schemeSpecificPart.charAt(0)!='/' || parsed.schemeSpecificPart.charAt(1)!='/') {
			throw "Generic URI values should start with '//':"+parsed.spec;
		}

		var work = parsed.schemeSpecificPart.substring(2);
		var pathStart = work.indexOf("/");
		parsed.authority = pathStart<0 ? work : work.substring(0,pathStart);
		parsed.path = pathStart<0 ? "" : work.substring(pathStart);
		var hash = parsed.path.indexOf('#');
		if (hash>=0) {
			parsed.fragment = parsed.path.substring(hash+1);
			parsed.path = parsed.path.substring(0,hash);
		}
		var questionMark = parsed.path.indexOf('?');
		if (questionMark>=0) {
			parsed.query = parsed.path.substring(questionMark+1);
			parsed.path = parsed.path.substring(0,questionMark);
		}
		if (parsed.path=="/" || parsed.path=="") {
			parsed.segments = [];
		} else {
			parsed.segments = parsed.path.split(/\//);
			if (parsed.segments.length>0 && parsed.segments[0]=='' && parsed.path.length>1 && parsed.path.charAt(1)!='/') {
				// empty segment at the start, remove it
				parsed.segments.shift();
			}
			if (parsed.segments.length>0 && parsed.path.length>0 && parsed.path.charAt(parsed.path.length-1)=='/' && parsed.segments[parsed.segments.length-1]=='') {
				// we may have an empty the end
				// check to see if it is legimate
				if (parsed.path.length>1 && parsed.path.charAt(parsed.path.length-2)!='/') {
					parsed.segments.pop();
				}
			}
		}
		parsed.isGeneric = true;
	};

	this.newBlankNode = function() {
		this.blankCounter++;
		return "_:" + this.blankCounter;
	};

	this.trim = function(str) {
		return str.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
	};

	this.tokenize = function(str) {
		return this.trim(str).split(/\s+/);
	};

	this.parseSafeCURIEOrCURIEOrURI = function(value,prefixes,base) {
		value = this.trim(value);
		if (value.charAt(0)=='[' && value.charAt(value.length-1)==']') {
			value = value.substring(1,value.length-1);
			value = value.trim(value);
			if (value.length==0) {
				return null;
			}
			return this.parseCURIE(value,prefixes,base);
		}
		else {
			return this.parseCURIEOrURI(value,prefixes,base);
		}
	};

	this.parseCURIE = function(value,prefixes,base) {
		var 
			colon = value.indexOf(":"),
			uri
		;
		if (colon>=0) {
			var prefix = value.substring(0,colon);
			if (prefix=="") {
				// default prefix
				uri = prefixes[""];
				return uri ? uri+value.substring(colon+1) : null;
			} else if (prefix=="_") {
				// blank node
				return "_:"+value.substring(colon+1);
			} else if (this.NCNAME.test(prefix)) {
				uri = prefixes[prefix];
				if (uri) {
					return uri+value.substring(colon+1);
				}
			}
		}
		return null;
	};

	this.parseCURIEOrURI = function(value,prefixes,base) {
		var curie = this.parseCURIE(value,prefixes,base);
		if (curie) {
			return curie;
		}
		return this.resolveAndNormalize(base,value);
	};

	this.parsePredicate = function(value,defaultVocabulary,terms,prefixes,base) {
		var predicate = this.parseTermOrCURIEOrAbsURI(value,defaultVocabulary,terms,prefixes,base);
		if (predicate && predicate.indexOf("_:")==0) {
			return null;
		}
		return predicate;
	};

	this.parseTermOrCURIEOrURI = function(value,defaultVocabulary,terms,prefixes,base) {
		value = this.trim(value);
		var curie = this.parseCURIE(value,prefixes,base);
		if (curie) {
			return curie;
		} else {
			var term = terms[value];
			if (term) {
				return term;
			}
			var lcvalue = value.toLowerCase();
			term = terms[lcvalue];
			if (term) {
				return term;
			}
			if (defaultVocabulary && !this.absURIRE.exec(value)) {
				return defaultVocabulary+value
			}
		}
		return this.resolveAndNormalize(base,value);
	};

	this.parseTermOrCURIEOrAbsURI = function(value,defaultVocabulary,terms,prefixes,base) {
		value = this.trim(value);
		var curie = this.parseCURIE(value,prefixes,base);
		if (curie) {
			return curie;
		} else {
			if (defaultVocabulary && !this.absURIRE.exec(value)) {
				return defaultVocabulary+value
			}
			var term = terms[value];
			if (term) {
				return term;
			}
			var lcvalue = value.toLowerCase();
			term = terms[lcvalue];
			if (term) {
				return term;
			}
		}
		if (this.absURIRE.exec(value)) {
			return this.resolveAndNormalize(base,value);
		}
		return null;
	};

	this.resolveAndNormalize = function(base,href) {
		var u = base.resolve(href);
		var parsed = this.parseURI(u);
		parsed.normalize();
		return parsed.spec;
	};

	this.parsePrefixMappings = function(str,target) {
		var values = this.tokenize(str);
		var prefix = null;
		var uri = null;
		for (var i=0; i<values.length; i++) {
			if (values[i][values[i].length-1]==':') {
				prefix = values[i].substring(0,values[i].length-1);
			} else if (prefix) {
				target[prefix] = values[i];
				prefix = null;
			}
		}
	};

	this.copyMappings = function(mappings) {
		var newMappings = {};
		for (var k in mappings) {
			newMappings[k] = mappings[k];
		}
		return newMappings;
	};

	this.ancestorPath = function(node) {
		var path = "";
		while (node && node.nodeType!=Node.DOCUMENT_NODE) {
			path = "/"+node.localName+path;
			node = node.parentNode;
		}
		return path;
	};

	this.push = function(parent, subject) {
		return {
			parent: parent,
			subject: subject ? subject : (parent ? parent.subject : null),
			parentObject: null,
			incomplete: [],
			listMapping: parent ? parent.listMapping : {},
			language: parent ? parent.language : this.language,
			prefixes: parent ? parent.prefixes : this.prefixes,
			terms: parent ? parent.terms : this.terms,
			vocabulary: parent ? parent.voabulary : this.vocabulary
	   };
	};
	
	this.process = function(node) {
		var 
			queue = [],
			i,
			value,
			values,
			predicate,
			list
		;
		queue.push({ current: node, context: this.push(null,this.getNodeBase(node))});
		while (queue.length>0) {
		  var item = queue.shift();
		  if (item.parent) {
			 // Sequence Step 14: list triple generation
			 if (item.context.parent && item.context.parent.listMapping==item.listMapping) {
				// Skip a child context with exactly the same mapping
				continue;
			 }
			 //console.log("Generating lists for "+item.subject+", tag "+item.parent.localName);
			 for (predicate in item.listMapping) {
				list = item.listMapping[predicate];
				if (list.length==0) {
				   this.addTriple(item.parent,item.subject,predicate,{ type: this.objectURI, value: "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil" });
				   continue;
				}
				var bnodes = [];
				for (i=0; i<list.length; i++) {
				   bnodes.push(this.newBlankNode());
				}
				for (i=0; i<bnodes.length; i++) {
				   this.addTriple(item.parent,bnodes[i],"http://www.w3.org/1999/02/22-rdf-syntax-ns#first",list[i]);
				   this.addTriple(item.parent,bnodes[i],"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest",{ type: this.objectURI , value: (i+1)<bnodes.length ? bnodes[i+1] : "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil" });
				}
				this.addTriple(item.parent,item.subject,predicate,{ type: this.objectURI, value: bnodes[0] });
			 }
			 continue;
		  }
		  var current = item.current;
		  var context = item.context;

		  //console.log("Tag: "+current.localName+", listMapping="+JSON.stringify(context.listMapping));

		  // Sequence Step 1
		  var skip = false;
		  var newSubject = null;
		  var currentObjectResource = null;
		  var typedResource = null;
		  var prefixes = context.prefixes;
		  var prefixesCopied = false;
		  var incomplete = [];
		  var listMapping = context.listMapping;
		  var listMappingDifferent = context.parent ? false : true;
		  var language = context.language;
		  var vocabulary = context.vocabulary;

		  // TODO: the "base" element may be used for HTML+RDFa 1.1
		  var base = this.parseURI(this.getNodeBase(current));
		  current.subject = null;

		  // Sequence Step 2: set the default vocabulary
		  var vocabAtt = current.getAttributeNode("vocab");
		  if (vocabAtt) {
			 value = this.trim(vocabAtt.value);
			 if (value.length>0) {
				vocabulary = value;
				var baseSubject = base.spec;
				this.addTriple(current,baseSubject,"http://www.w3.org/ns/rdfa#usesVocabulary",{ type: this.objectURI , value: vocabulary});
			 } else {
				vocabulary = this.vocabulary;
			 }
		  }

		  // Sequence Step 3: IRI mappings
		  // handle xmlns attributes
		  for (i=0; i<current.attributes.length; i++) {
			 var att = current.attributes[i];
			 //if (att.namespaceURI=="http://www.w3.org/2000/xmlns/") {
			 if (att.nodeName.charAt(0)=="x" && att.nodeName.indexOf("xmlns:")==0) {
				if (!prefixesCopied) {
				   prefixes = this.copyMappings(prefixes);
				   prefixesCopied = true;
				}
				var prefix = att.nodeName.substring(6);
				// TODO: resolve relative?
				prefixes[prefix] = this.trim(att.value);
			 }
		  }
		  // Handle prefix mappings (@prefix)
		  var prefixAtt = current.getAttributeNode("prefix");
		  if (prefixAtt) {
			 if (!prefixesCopied) {
				prefixes = this.copyMappings(prefixes);
				prefixesCopied = true;
			 }
			 this.parsePrefixMappings(prefixAtt.value,prefixes);
		  }


		  // Sequence Step 4: language
		  var xmlLangAtt = null;
		  for (i=0; !xmlLangAtt && i<this.langAttributes.length; i++) {
			 xmlLangAtt = current.getAttributeNodeNS(this.langAttributes[i].namespaceURI,this.langAttributes[i].localName);
		  }
		  if (xmlLangAtt) {
			 value = this.trim(xmlLangAtt.value);
			 if (value.length>0) {
				language = value;
			 }
		  }

		  var relAtt = current.getAttributeNode("rel");
		  var revAtt = current.getAttributeNode("rev");
		  var typeofAtt = current.getAttributeNode("typeof");
		  var propertyAtt = current.getAttributeNode("property");
		  var datatypeAtt = current.getAttributeNode("datatype");
		  var contentAtt = null;
		  for (i=0; !contentAtt && i<this.contentAttributes.length; i++) {
			 contentAtt = current.getAttributeNode(this.contentAttributes[i]);
		  }
		  var aboutAtt = current.getAttributeNode("about");
		  var srcAtt = current.getAttributeNode("src");
		  var resourceAtt = current.getAttributeNode("resource");
		  var hrefAtt = current.getAttributeNode("href");
		  var inlistAtt = current.getAttributeNode("inlist");

		  if (relAtt || revAtt) {
			 // Sequence Step 6: establish new subject and value
			 if (aboutAtt) {
				newSubject = this.parseSafeCURIEOrCURIEOrURI(aboutAtt.value,prefixes,base);
			 }
			 if (typeofAtt) {
				typedResource = newSubject;
			 }
			 if (!newSubject) {
				if (current.parentNode.nodeType==Node.DOCUMENT_NODE) {
				   newSubject = this.getNodeBase(current);
				} else if (context.parentObject) {
				   // TODO: Verify: If the xml:base has been set and the parentObject is the baseURI of the parent, then the subject needs to be the new base URI
				   newSubject = this.getNodeBase(current.parentNode)==context.parentObject ? this.getNodeBase(current) : context.parentObject;
				}
			 }
			 if (resourceAtt) {
				currentObjectResource = this.parseSafeCURIEOrCURIEOrURI(resourceAtt.value,prefixes,base);
			 }

			 if (!currentObjectResource) {
				if (hrefAtt) {
				   currentObjectResource = this.resolveAndNormalize(base,hrefAtt.value);
				} else if (srcAtt) {
				   currentObjectResource = this.resolveAndNormalize(base,srcAtt.value);
				} else if (typeofAtt && !aboutAtt && !(this.inXHTMLMode && (current.localName=="head" || current.localName=="body"))) {
				   currentObjectResource = this.newBlankNode();
				}
			 }
			 if (typeofAtt && !aboutAtt && this.inXHTMLMode && (current.localName=="head" || current.localName=="body")) {
				typedResource = newSubject;
			 } else if (typeofAtt && !aboutAtt) {
				typedResource = currentObjectResource;
			 }

		  } else if (propertyAtt && !contentAtt && !datatypeAtt) {
			 // Sequence Step 5.1: establish a new subject
			 if (aboutAtt) {
				newSubject = this.parseSafeCURIEOrCURIEOrURI(aboutAtt.value,prefixes,base);
				if (typeofAtt) {
				   typedResource = newSubject;
				}
			 }
			 if (!newSubject && current.parentNode.nodeType==Node.DOCUMENT_NODE) {
				newSubject = this.getNodeBase(current);
				if (typeofAtt) {
				   typedResource = newSubject;
				}
			 } else if (!newSubject && context.parentObject) {
				// TODO: Verify: If the xml:base has been set and the parentObject is the baseURI of the parent, then the subject needs to be the new base URI
				newSubject = this.getNodeBase(current.parentNode)==context.parentObject ? this.getNodeBase(current) : context.parentObject;
			 }
			 if (typeofAtt && !typedResource) {
				if (resourceAtt) {
				   typedResource = this.parseSafeCURIEOrCURIEOrURI(resourceAtt.value,prefixes,base);
				}
				if (!typedResource &&hrefAtt) {
				   typedResource = this.resolveAndNormalize(base,hrefAtt.value);
				}
				if (!typedResource && srcAtt) {
				   typedResource = this.resolveAndNormalize(base,srcAtt.value);
				}
				if (!typedResource && this.inXHTMLMode && (current.localName=="head" || current.localName=="body")) {
				   typedResource = newSubject;
				}
				if (!typedResource) {
				   typedResource = this.newBlankNode();
				}
				currentObjectResource = typedResource;
			 }
			 //console.log(current.localName+", newSubject="+newSubject+", typedResource="+typedResource+", currentObjectResource="+currentObjectResource);
		  } else {
			 // Sequence Step 5.2: establish a new subject
			 if (aboutAtt) {
				newSubject = this.parseSafeCURIEOrCURIEOrURI(aboutAtt.value,prefixes,base);
			 }
			 if (!newSubject && resourceAtt) {
				newSubject = this.parseSafeCURIEOrCURIEOrURI(resourceAtt.value,prefixes,base);
			 }
			 if (!newSubject && hrefAtt) {
				newSubject = this.resolveAndNormalize(base,hrefAtt.value);
			 }
			 if (!newSubject && srcAtt) {
				newSubject = this.resolveAndNormalize(base,srcAtt.value);
			 }
			 if (!newSubject) {
				if (current.parentNode && current.parentNode.nodeType==Node.DOCUMENT_NODE) {
				   newSubject = this.getNodeBase(current);
				} else if (this.inXHTMLMode && (current.localName=="head" || current.localName=="body")) {
				   newSubject = this.getNodeBase(current.parentNode)==context.parentObject ? this.getNodeBase(current) : context.parentObject;
				} else if (typeofAtt) {
				   newSubject = this.newBlankNode();
				} else if (context.parentObject) {
				   // TODO: Verify: If the xml:base has been set and the parentObject is the baseURI of the parent, then the subject needs to be the new base URI
				   newSubject = this.getNodeBase(current.parentNode)==context.parentObject ? this.getNodeBase(current) : context.parentObject;
				   if (!propertyAtt) {
					  skip = true;
				   }
				}
			 }
			 if (typeofAtt) {
				typedResource = newSubject;
			 }
		  }

		  //console.log(current.tagName+": newSubject="+newSubject+", currentObjectResource="+currentObjectResource+", typedResource="+typedResource+", skip="+skip);

		  if (newSubject) {
			 current.subject = newSubject;
			 if (typeofAtt && !aboutAtt && currentObjectResource) {
				current.subject = currentObjectResource;
			 }
		  }

		  // Sequence Step 7: generate type triple
		  if (typedResource) {
			 values = this.tokenize(typeofAtt.value);
			 for (i=0; i<values.length; i++) {
				var object = this.parseTermOrCURIEOrAbsURI(values[i],vocabulary,context.terms,prefixes,base);
				if (object) {
				   this.addTriple(current,typedResource,this.typeURI,{ type: this.objectURI , value: object});
				}
			 }
		  }

		  // Sequence Step 8: new list mappings if there is a new subject
		  //console.log("Step 8: newSubject="+newSubject+", context.parentObject="+context.parentObject);
		  if (newSubject && newSubject!=context.parentObject) {
			 //console.log("Generating new list mapping for "+newSubject);
			 listMapping = {};
			 listMappingDifferent = true;
		  }

		  // Sequence Step 9: generate object triple
		  if (currentObjectResource) {
			 if (relAtt && inlistAtt) {
				values = this.tokenize(relAtt.value);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  list = listMapping[predicate];
					  if (!list) {
						 list = [];
						 listMapping[predicate] = list;
					  }
					  list.push({ type: this.objectURI, value: currentObjectResource });
				   }
				}
			 } else if (relAtt) {
				values = this.tokenize(relAtt.value);
				//alert(newSubject+" "+relAtt.value+" "+currentObjectResource+" "+values.length);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  this.addTriple(current,newSubject,predicate,{ type: this.objectURI, value: currentObjectResource});
				   }
				}
			 }
			 if (revAtt) {
				values = this.tokenize(revAtt.value);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  this.addTriple(current,currentObjectResource, predicate, { type: this.objectURI, value: newSubject});
				   }
				}
			 }
		  } else {
			 // Sequence Step 10: incomplete triples
			 if (newSubject && !currentObjectResource && (relAtt || revAtt)) {
				currentObjectResource = this.newBlankNode();
				//alert(current.tagName+": generated blank node, newSubject="+newSubject+" currentObjectResource="+currentObjectResource);
			 }
			 if (relAtt && inlistAtt) {
				values = this.tokenize(relAtt.value);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  list = listMapping[predicate];
					  if (!list) {
						 list = [];
						 listMapping[predicate] = list;
					  }
					  //console.log("Adding incomplete list for "+predicate);
					  incomplete.push({ predicate: predicate, list: list });
				   }
				}
			 } else if (relAtt) {
				values = this.tokenize(relAtt.value);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  incomplete.push({ predicate: predicate, forward: true });
				   }

				}
			 }
			 if (revAtt) {
				values = this.tokenize(revAtt.value);
				for (i=0; i<values.length; i++) {
				   predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				   if (predicate) {
					  incomplete.push({ predicate: predicate, forward: false });
				   }
				}
			 }
		  }
		  
		  // Sequence Step 12: complete incomplete triples with new subject
		  if (newSubject && !skip) {
			 for (i=0; i<context.incomplete.length; i++) {
				if (context.incomplete[i].list) {
				   //console.log("Adding subject "+newSubject+" to list for "+context.incomplete[i].predicate);
				   // TODO: it is unclear what to do here
				   context.incomplete[i].list.push({ type: this.objectURI, value: newSubject });
				} else if (context.incomplete[i].forward) {
				   //console.log(current.tagName+": completing forward triple "+context.incomplete[i].predicate+" with object="+newSubject);
				   this.addTriple(current,context.subject,context.incomplete[i].predicate, { type: this.objectURI, value: newSubject});
				} else {
				   //console.log(current.tagName+": completing reverse triple with object="+context.subject);
				   this.addTriple(current,newSubject,context.incomplete[i].predicate,{ type: this.objectURI, value: context.subject});
				}
			 }
		  }

		  // Step 11: Current property values
		  if (propertyAtt) {
			 // TODO: for HTML+RDFa 1.1, the datatype must be set if the content comes from the datetime attribute
			 //alert(this.getNodeBase(current)+" "+newSubject+" "+propertyAtt.value);
			 var datatype = null;
			 var content = null; 
			 if (datatypeAtt) {
				datatype = datatypeAtt.value=="" ? this.PlainLiteralURI : this.parseTermOrCURIEOrAbsURI(datatypeAtt.value,vocabulary,context.terms,prefixes,base);
				content = datatype==this.XMLLiteralURI ? null : (contentAtt ? contentAtt.value : current.textContent);
			 } else if (contentAtt) {
				datatype = this.PlainLiteralURI;
				content = contentAtt.value;
			 } else if (!relAtt && !revAtt && !contentAtt) {
				if (resourceAtt) {
				   content = this.parseSafeCURIEOrCURIEOrURI(resourceAtt.value,prefixes,base);
				}
				if (!content && hrefAtt) {
				   content = this.resolveAndNormalize(base,hrefAtt.value);
				} else if (!content && srcAtt) {
				   content = this.resolveAndNormalize(base,srcAtt.value);
				}
				if (content) {
				   datatype = this.objectURI;
				}
			 }
			 if (!datatype) {
				if (typeofAtt && !aboutAtt) {
				   datatype = this.objectURI;
				   content = typedResource;
				} else {
				   datatype = this.PlainLiteralURI;
				   content = current.textContent;
				}
			 }
			 values = this.tokenize(propertyAtt.value);
			 for (i=0; i<values.length; i++) {
				predicate = this.parsePredicate(values[i],vocabulary,context.terms,prefixes,base);
				if (predicate) {
				   if (inlistAtt) {
					  list = listMapping[predicate];
					  if (!list) {
						 list = [];
						 listMapping[predicate] = list;
					  }
					  list.push(datatype==this.XMLLiteralURI ? { type: this.XMLLiteralURI, value: current.childNodes} : { type: datatype ? datatype : this.PlainLiteralURI, value: content, language: language});
				   } else {
					  if (datatype==this.XMLLiteralURI) {
						 this.addTriple(current,newSubject,predicate,{ type: this.XMLLiteralURI, value: current.childNodes});
					  } else {
						 this.addTriple(current,newSubject,predicate,{ type: datatype ? datatype : this.PlainLiteralURI, value: content, language: language});
						 //console.log(newSubject+" "+predicate+"="+content);
					  }
				   }
				}
			 }
		  }

		  var childContext = null;
		  var listSubject = newSubject;
		  if (skip) {
			 // TODO: should subject be null?
			 childContext = this.push(context,context.subject);
			 // TODO: should the entObject be passed along?  If not, then intermediary children will keep properties from being associated with incomplete triples.
			 // TODO: Verify: if the current baseURI has changed and the parentObject is the parent's base URI, then the baseURI should change
			 childContext.parentObject = this.getNodeBase(current.parentNode)==context.parentObject ? this.getNodeBase(current) : context.parentObject;
			 childContext.incomplete = context.incomplete;
			 childContext.language = language;
			 childContext.prefixes = prefixes;
			 childContext.vocabulary = vocabulary;
		  } else {
			 childContext = this.push(context,newSubject);
			 childContext.parentObject = currentObjectResource ? currentObjectResource : (newSubject ? newSubject : context.subject);
			 childContext.prefixes = prefixes;
			 childContext.incomplete = incomplete;
			 if (currentObjectResource) {
				//console.log("Generating new list mapping for "+currentObjectResource);
				listSubject = currentObjectResource;
				listMapping = {};
				listMappingDifferent = true;
			 }
			 childContext.listMapping = listMapping;
			 childContext.language = language;
			 childContext.vocabulary = vocabulary;
		  }
		  if (listMappingDifferent) {
			 //console.log("Pushing list parent "+current.localName);
			 queue.unshift({ parent: current, context: context, subject: listSubject, listMapping: listMapping});
		  }
		  for (var child = current.lastChild; child; child = child.previousSibling) {
			 if (child.nodeType==Node.ELEMENT_NODE) {
				//console.log("Pushing child "+child.localName);
				queue.unshift({ current: child, context: childContext});
			 }
		  }
	   }
	   
	};
	
	/**
	 * Sets a single namespace and prefix
	 */
	this.setMapping = function(prefix, namespace) {
		this.prefixes[prefix] = namespace;
		return this;
	};
	
	/**
	 * Returns the current prefix/namespace state
	 */
	this.getMappings = function() {
		return this.prefixes;
	};
	
	/**
	 * Parses a DOM node for RDFa and calls the callback function for each triple
	 */
	this.parse = function(node, callback) {
		// node check
		if (!node || !node.nodeType) {
			throw "First parameter of parse(node, callback) must be a DOM node.";
		}
		if (node.nodeType == Node.DOCUMENT_NODE) {
			node = node.documentElement;
		}
		// callback check
		if (typeof callback != "function") {
			throw "Second parameter of parse(node, callback) must be a function.";
		}
		this.callback = callback;
		try {
			this.setContext(node);
			this.process(node);
			return true;
		}
		catch (e) {
			if (window.console !== undefined && console.log) {
				console.log(e);
				console.log(e.stack);
			}
			return false;
		}
	};

	/**
	 * Returns the document object associated with the given node.
	 */
	this.getNodeDocument = function(node) {
		if (node.nodeType == Node.DOCUMENT_NODE) {
			return node;
		}
		else if (node.parentNode) {
			return this.getNodeDocument(node.parentNode);
		}
		else if (node.ownerDocument) {
			return node.ownerDocument;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Initializes the parser context.
	 */
	this.setContext = function(node) {
		this.inXHTMLMode = false;// handle node as HTML/HTML5
		
		this.langAttributes = [
			{ namespaceURI: "http://www.w3.org/XML/1998/namespace", localName: "lang" },
			{ namespaceURI: null, localName: "lang" }
		];
		this.contentAttributes = [ "value", "datetime", "content" ];

		this.language = this.getInitialLanguage(node);
		this.vocabulary = this.getInitialVocabulary(node);
		this.setInitialPrefixes(node);

		this.terms = {
			"describedby": "http://www.w3.org/2007/05/powder-s#describedby",
			"license": "http://www.w3.org/1999/xhtml/vocab#license",
			"role": "http://www.w3.org/1999/xhtml/vocab#role"
		};
	};
	
	/**
	 * Retrieves the language context for the given node.
	 */
	this.getInitialLanguage = function(node) {
		var result, doc;
		// try current
		for (var i = 0, imax = this.langAttributes.length; i < imax; i++) {
			result = node.getAttributeNS(this.langAttributes[i].namespaceURI, this.langAttributes[i].localName);
			if (result) {
				return result;
			}
		}
		// try parent
		if (node.parentNode && node.parentNode.nodeType != Node.DOCUMENT_NODE) {
			return this.getInitialLanguage(node.parentNode);
		}
		// try associated doc element
		else if ((doc = this.getNodeDocument(node)) && doc.documentElement != node) {
			return this.getInitialLanguage(doc.documentElement);
		}
		return null;
	};
	
	/**
	 * Sets the prefix/namespace context for the given node.
	 */
	this.setInitialPrefixes = function(node) {
		var doc, i, imax, j;
		// set current's
		for (i = 0, imax = node.attributes.length; i < imax; i++) {
			var attr = node.attributes[i];
			if (attr.nodeName && attr.nodeName.match(/^xmlns\:/)) {
				var prefix = attr.nodeName.substring(6);
				if (!this.prefixes[prefix]) {
					this.prefixes[prefix] = this.trim(attr.value);
				}
			}
			else if (attr.nodeName && attr.nodeName == "prefix") {
				var defs = {};
				this.parsePrefixMappings(attr.value, defs);
				for (j in defs) {
					if (!this.prefixes[j]) {
						this.prefixes[j] = defs[j];
					}
				}
			}
		}
		// set parent's
		if (node.parentNode && node.parentNode.nodeType != Node.DOCUMENT_NODE) {
			this.setInitialPrefixes(node.parentNode);
		}
		// set doc's'
		else if ((doc = this.getNodeDocument(node)) && doc.documentElement != node) {
			this.setInitialPrefixes(doc.documentElement);
		}
		// set defaults
		else {
			for (i in this.defaultPrefixes) {
				if (typeof this.prefixes[i] == "undefined") {
					this.prefixes[i] = this.defaultPrefixes[i];
				}
			}
		}
	};
	
	/**
	 * Retrieves the initial vocabulary context for the given node.
	 */
	this.getInitialVocabulary = function(node) {
		var doc, result;
		// try current
		if ((result = node.getAttribute("vocab"))) {
			return result;
		}
		// try parent
		if (node.parentNode && node.parentNode.nodeType != Node.DOCUMENT_NODE) {
			return this.getInitialVocabulary(node.parentNode);
		}
		// try doc
		else if ((doc = this.getNodeDocument(node)) && doc.documentElement != node) {
			return this.getInitialVocabulary(doc.documentElement);
		}
		return null;
	};
	
	/**
	 * Tries to detect a baseURI for the given node.
	 */
	this.getNodeBase = function(node) {
		// read-only property already set by browser
		if (node.baseURI) return node.baseURI;
		// try via doc property
		var doc = this.getNodeDocument(node);
		if (doc && doc.baseURI) {
			return doc.baseURI;
		}
		// try via base tags in doc
		var els;
		if (doc && (els = doc.getElementsByTagName("base"))) {
			return els[0].getAttribute("href");
		}
		// try window
		if (window && window.location) {
			return window.location.href;
		}
		return null;
	};

	/**
	 * Calls the callback when a new triple is extracted.
	 */
	this.addTriple = function(origin, subject, predicate, object) {
		var datatype = null;
		var language = object.language || null;
		if (object.type == this.PlainLiteralURI) {
			datatype = language ? this.prefixes['rdf'] + 'langString' : this.prefixes['xsd'] + 'string';
		}
		else if (object.type == this.XMLLiteralURI) {
			datatype = object.type;
		}
		this.callback.call(origin, subject, predicate, object.value, datatype, language);
	};	

}
