PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl:<http://www.w3.org/2002/07/owl#>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX sd:<http://www.w3.org/ns/sparql-service-description#>
PREFIX void:<http://rdfs.org/ns/void#>
PREFIX foaf:<http://xmlns.com/foaf/0.1/>
PREFIX msg:<http://www.openrdf.org/rdf/2011/messaging#>
PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>
PREFIX prov:<http://www.w3.org/ns/prov#>
PREFIX audit:<http://www.openrdf.org/rdf/2012/auditing#>

INSERT {
<../> calli:hasComponent <../query-view.js>.
<../query-view.js> a <types/PURL>, calli:PURL ;
	rdfs:label "query-view.js";
	calli:alternate <scripts/query-view?minified>;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.js> }
};

DELETE {
<../query-view.js> calli:alternate <scripts/query-view.js>
} INSERT {
<../query-view.js> calli:alternate <scripts/query_bundle?minified>
} WHERE {
    <../query-view.js> calli:alternate <scripts/query-view.js>
};

DELETE {
<../query-view.js> calli:alternate <scripts/query-view?minified>
} INSERT {
<../query-view.js> calli:alternate <scripts/query_bundle?minified>
} WHERE {
<../query-view.js> calli:alternate <scripts/query-view?minified>
};

INSERT {
<../> calli:hasComponent <../query-view.css>.
<../query-view.css> a <types/PURL>, calli:PURL ;
	rdfs:label "query-view.css";
	calli:alternate <styles/callimachus-query-view.less?less>;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.css> }
};

DELETE {
	<../document-editor.html> calli:alternate ?oldalt
} INSERT {
	<../document-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/ckeditor.html>) AS ?oldalt)
    BIND (str(<pages/document-editor.html>) AS ?newalt)
	<../document-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../css-editor.html> calli:alternate ?oldalt
} INSERT {
	<../css-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#css>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#css>) AS ?newalt)
	<../css-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../html-editor.html> calli:alternate ?oldalt
} INSERT {
	<../html-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#html>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#html>) AS ?newalt)
	<../html-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../javascript-editor.html> calli:alternate ?oldalt
} INSERT {
	<../javascript-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#javascript>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#javascript>) AS ?newalt)
	<../javascript-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../sparql-editor.html> calli:alternate ?oldalt
} INSERT {
	<../sparql-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#sparql>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#sparql>) AS ?newalt)
	<../sparql-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../text-editor.html> calli:alternate ?oldalt
} INSERT {
	<../text-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html>) AS ?oldalt)
    BIND (str(<pages/text-editor.html>) AS ?newalt)
	<../text-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../xml-editor.html> calli:alternate ?oldalt
} INSERT {
	<../xml-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#xml>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#xml>) AS ?newalt)
	<../xml-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../xquery-editor.html> calli:alternate ?oldalt
} INSERT {
	<../xquery-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#xquery>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#xquery>) AS ?newalt)
	<../xquery-editor.html> calli:alternate ?oldalt
};

