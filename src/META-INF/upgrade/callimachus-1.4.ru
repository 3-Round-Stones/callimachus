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
<../> calli:hasComponent <../markdown-editor.html> .
<../markdown-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "markdown-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#markdown>) AS ?alternate)
	FILTER NOT EXISTS { <../markdown-editor.html> a calli:Purl }
};

# Setup process determins upgrade file based on versionInfo
DELETE {
	</callimachus/ontology> owl:versionInfo "1.4"
} INSERT {
	</callimachus/ontology> owl:versionInfo "1.4"
} WHERE {
	</callimachus/ontology> owl:versionInfo "1.4"
};

