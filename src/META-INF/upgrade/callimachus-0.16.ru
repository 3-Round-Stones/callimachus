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
PREFIX audit:<http://www.openrdf.org/rdf/2009/auditing#>

DELETE {
	</main+menu> calli:item </main+menu#resources>
} WHERE {
	</main+menu> calli:item </main+menu#resources>
	FILTER NOT EXISTS { </main+menu#resources> rdfs:label ?label }
};

DELETE {
	</main+menu> calli:item </main+menu#administration>
} WHERE {
	</main+menu> calli:item </main+menu#administration>
	FILTER NOT EXISTS { </main+menu#administration> rdfs:label ?label }
};

INSERT {
	?manager a </callimachus/DigestManager>, calli:DigestManager
} WHERE {
	?manager a </callimachus/AccountManager>, calli:AccountManager
	FILTER NOT EXISTS { ?manager a </callimachus/DigestManager> }
};

DELETE {
	</callimachus/> rdfs:label "callimachus/"
} INSERT {
	</callimachus/> rdfs:label "callimachus"
} WHERE {
	</callimachus/> rdfs:label "callimachus/"
};
