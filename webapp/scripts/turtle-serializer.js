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

    this.baseBuffer = '';
    this.prefixes = {};

    this.subjectBuffer = {};
    this.prefixBuffer = {};

    this.setBaseUri = function(baseUri) {
        this.baseBuffer = '@base <' + baseUri + '>.';
    };

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

    this.buildUri = function(uri) {
        if (uri == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") return 'a';
        var namespace = uri.replace(/[\w_\-\.\\%]+$/, '');
        for (var prefix in this.prefixes) {
            if (prefix && this.prefixes[prefix] == namespace) {
                this.prefixBuffer[prefix] = '@prefix ' + prefix + ':<' + namespace + '>.';
                return prefix + ':' + uri.substring(namespace.length);
            }
        }
        return '<' + uri + '>';
    };

    this.buildBnode = function(id) {
        return '_:' + id;
    };

    this.buildObject = function(p, o) {
        var pTerm = this.buildUri(p.value);
        var oTerm;
        if (o.type != 'literal') {
            oTerm = o.type == 'bnode' ? this.buildBnode(o.value) : this.buildUri(o.value);
            return pTerm + ' ' + oTerm;
        }
        else if (o.datatype == this.XMLLiteralURI) {
            oTerm = this.buildXMLLiteral(o.value);
            return pTerm + ' ' + oTerm;
        }
        else if (o.datatype == this.LangStringURI) {
            oTerm = this.buildLiteral(o.value);
            return pTerm + ' ' + oTerm + '@' + o["xml:lang"];
        }
        else if (o.datatype == this.StringURI || !o.datatype) {
            oTerm = this.buildLiteral(o.value);
            return pTerm + ' ' + oTerm;
        }
        else {
            oTerm = this.buildLiteral(o.value);
            var dTerm = this.buildUri(o.datatype);
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
    
    this.addTriple= function(triple) {
        var term = triple.s.type == 'bnode' ? this.buildBnode(triple.s.value) : this.buildUri(triple.s.value);
        if (!this.subjectBuffer[term]) {
            this.subjectBuffer[term] = [];
        }
        this.subjectBuffer[term].push(this.buildObject(triple.p, triple.o));
    };
    
    this.toString = function() {
        // generate doc
        var result = [];
        // base
        if (this.baseBuffer) result.push(this.baseBuffer);
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
