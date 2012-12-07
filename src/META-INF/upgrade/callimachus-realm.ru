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
	$realm a calli:Realm, calli:Folder;
		rdfs:label ?label;
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
		calli:contributor </auth/groups/users>;
		calli:editor </auth/groups/staff>;
		calli:administrator </auth/groups/admin>;
		calli:unauthorized <../unauthorized.html>;
		calli:forbidden <../forbidden.html>;
		calli:layout <../default-layout.xq>.
} WHERE {
	FILTER NOT EXISTS { $realm a calli:Realm }
	BIND (replace(replace(str($realm), "^[a-z]*://", ""), "/$", "") AS ?label)
};

INSERT {
	$realm a <types/Realm>.
} WHERE {
	FILTER NOT EXISTS { $realm a <types/Origin> }
};

INSERT {
	$realm calli:authentication </auth/digest+account>
} WHERE {
	FILTER NOT EXISTS { $realm calli:authentication ?accounts }
};
