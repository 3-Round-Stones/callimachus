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
<../> calli:hasComponent <../xquery-editor.html>.
<../xquery-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "xquery-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<editor/text-editor.html#xquery>) AS ?alternate)
	FILTER NOT EXISTS { <../xquery-editor.html> a calli:PURL }
};

DELETE {
    </auth/groups/system> calli:anonymousFrom ?host
} WHERE {
    </auth/groups/system> calli:anonymousFrom ?host
    FILTER (?host != "localhost")
    FILTER EXISTS { </auth/groups/system> calli:anonymousFrom "localhost" }
};

DELETE {
    <../scripts.js> calli:alternate ?source
} INSERT {
    <../scripts.js> calli:alternate ?minified
} WHERE {
    BIND (str(<scripts/index?minified>) AS ?minified)
    <../scripts.js> calli:alternate ?source
    FILTER (str(<scripts/index?source>) = ?source)
};

INSERT {
</auth/> calli:hasComponent </auth/credentials/>.
</auth/credentials/> a <types/Folder>, calli:Folder;
    rdfs:label "credentials";
    calli:editor </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/credentials/> a calli:Folder }
};

DELETE WHERE {
<../> calli:hasComponent <../query-view.js>.
<../query-view.js> a <types/PURL>, calli:PURL ;
	rdfs:label "query-view.js";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
};

DELETE {
</sparql> a <types/SparqlService>;
    rdfs:label "sparql".
} INSERT {
</sparql> a <types/Datasource>, calli:Datasource;
    rdfs:label "SPARQL";
    rdfs:comment "SPARQL endpoint to default dataset";
} WHERE {
</sparql> a <types/SparqlService>;
    rdfs:label "sparql".

};

DELETE WHERE {
	</> calli:hasComponent </describe>.
	</describe> a <types/DescribeService>; ?p ?o
};
