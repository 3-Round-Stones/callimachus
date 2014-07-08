/**
 * turtle-serializer.js - A basic RDF Turtle Serializer  
 */
/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

function TurtleSerializer() {
    
	this.XMLLiteralURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral"; 
	this.StringURI = "http://www.w3.org/2001/XMLSchema#string";
	this.LangStringURI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"; 
	
	this.prefixes = {
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

	this.subjectBuffer = {};
	this.prefixBuffer = {};
	
	this.prefixId = 0;
	
	/**
	 * Sets a single namespace and prefix
	 */
	this.setMapping = function(prefix, namespace) {
		this.prefixes[prefix] = namespace;
		return this;
	};
	
	/**
	 * Sets the prefix mappings
	 */
	this.setMappings = function(mappings, reset) {
		if (reset) {
			this.prefixes = mappings;
		}
		else {
			for (var prefix in mappings) {
				if (!this.prefixes[prefix]) {
					this.prefixes[prefix] = mappings[prefix];
				}
			}
		}
	};
	
	this.buildPName= function(uri) {
		var m = uri.match(/^(.+[\/\#]+)([^\/\#]+)$/);
		if (!m) {
			throw "Could not create prefixed name from '" + uri + "'";
		}
		return this.buildPrefix(m[1]) + ':' + m[2];
	};
	
	this.buildPrefix = function(namespace) {
		var result = null;
		for (var prefix in this.prefixes) {
			if (prefix && this.prefixes[prefix] == namespace) {
				result = prefix;
				break;
			}
		}
		if (!result) {
			while (this.prefixes['ns' + this.prefixId]) {
				this.prefixId++;
			}
			result = 'ns' + this.prefixId;
			this.prefixes[result] = namespace;
		}
		this.prefixBuffer[result] = '@prefix ' + result + ':<' + namespace + '>.';
		return result;
	};
	
	this.buildBnode = function(id) {
		return id;
	};
	
	this.buildUri = function(uri) {
		return '<' + uri + '>';
	};
	
	this.buildObject = function(predicate, object, datatype, language) {
		var pTerm = this.buildPName(predicate);
		var oTerm;
		if (!datatype) {
			oTerm = (object.match(/^_:/)) ? this.buildBnode(object) : this.buildUri(object);
			return pTerm + ' ' + oTerm;
		}
		else if (datatype == this.XMLLiteralURI) {
			oTerm = this.buildXMLLiteral(object);
			return pTerm + ' ' + oTerm;
		}
		else if (datatype == this.LangStringURI) {
			oTerm = this.buildLiteral(object);
			return pTerm + ' ' + oTerm + '@' + language;
		}
		else if (datatype == this.StringURI) {
			oTerm = this.buildLiteral(object);
			return pTerm + ' ' + oTerm;
		}
		else {
			oTerm = this.buildLiteral(object);
            var dTerm = this.buildPName(datatype);
			return pTerm + ' ' + oTerm + '^^' + dTerm;
		}
	};
	
	this.buildLiteral = function(value) {
		if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\t') >= 0) {
            return '"""' + value.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"""';
		} else {
            return '"' + value.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"';
		}
	};
	
	this.buildXMLLiteral = function(value) {
		return '"""' + value.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"""';
	};
	
	this.addTriple= function(subject, predicate, object, datatype, language) {
		var term = (subject.match(/^_:/)) ? this.buildBnode(subject) : this.buildUri(subject);
		if (!this.subjectBuffer[term]) {
			this.subjectBuffer[term] = [];
		}
		this.subjectBuffer[term].push(this.buildObject(predicate, object, datatype, language));
	};
	
	this.toString = function(triples) {
		// add triples, if provided
		if (triples) {
			for (var i = 0, imax = triples.length; i < imax; i++) {
				this.addTriple(triples[i].subject, triples[i].predicate, triples[i].object, triples[i].datatype, triples[i].language);
			}
		}
		// generate doc
		var result = [];
		// prefixes
		for (var prefix in this.prefixBuffer) {
			result.push(this.prefixBuffer[prefix]);
		}
		result.push('');
		// subjects
		for (var term in this.subjectBuffer) {
			result.push(term);
			result.push("\t" + this.subjectBuffer[term].join(";\n\t") +'.');
		}
		result.push('');
		return result.join("\n");
	};
	
}
