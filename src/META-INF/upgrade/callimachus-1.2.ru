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

