# callimachus-origin.ru
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

INSERT {
	?digest calli:authName ?name; calli:authNamespace ?space
} WHERE {
	{
		</> calli:authentication [calli:authName ?name; calli:authNamespace ?space]
	} UNION {
		FILTER NOT EXISTS { </> calli:authentication [calli:authName ?n; calli:authNamespace ?s] }
		BIND (replace(replace(str(</>), "^[a-z]*://", ""), "[:/].*", "") AS ?name)
		BIND (</auth/digest-users/> AS ?space)
	}
	FILTER NOT EXISTS { $origin a calli:Origin }
	BIND (iri(concat(str($origin),"auth/digest+account")) AS ?digest)
};

INSERT {
	$origin a <types/Origin>, calli:Origin;
		calli:authentication ?digest;
		calli:reader </auth/groups/system>;
		calli:hasComponent ?auth .
	?auth a <types/Folder>, calli:Folder;
	    rdfs:label "auth";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
	    calli:administrator </auth/groups/admin>;
	    calli:hasComponent ?digest .
	?digest a <types/DigestManager>, calli:DigestManager, calli:AuthenticationManager;
		rdfs:label "Digest account";
		rdfs:comment "Sign in with a username and password";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
		calli:administrator </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { $origin a calli:Origin }
	BIND (iri(concat(str($origin),"auth/")) AS ?auth)
	BIND (iri(concat(str($origin),"auth/digest+account")) AS ?digest)
};
