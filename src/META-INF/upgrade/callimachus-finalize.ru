# callimachus-finalize.ru
#
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

DELETE {
	?file calli:reader </auth/groups/public>
} INSERT {
	?file calli:reader </auth/groups/system>, </auth/groups/admin>
} WHERE {
	{
		<types/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/public>
	} UNION {
		<pipelines/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/public>
	} UNION {
		<queries/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/public>
	} UNION {
		<schemas/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/public>
	} UNION {
		<transforms/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/public>
	}
};

INSERT {
	<queries/folder-create-menu.rq> calli:reader </auth/groups/everyone>
} WHERE {
	FILTER NOT EXISTS { <queries/folder-create-menu.rq> calli:reader </auth/groups/everyone> }
};
