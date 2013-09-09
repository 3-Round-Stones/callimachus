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
	$origin a <types/Origin>, calli:Origin, calli:Realm, calli:Folder;
		rdfs:label ?label;
		calli:reader </auth/groups/public>,</auth/groups/system>;
		calli:subscriber </auth/groups/everyone>;
		calli:contributor </auth/groups/users>;
		calli:editor </auth/groups/staff>,</auth/groups/power>;
		calli:administrator </auth/groups/admin>;
		calli:unauthorized <../unauthorized.html>;
		calli:forbidden <../forbidden.html>;
		calli:layout <../default-layout.xq>;
		calli:error <../error.xpl>.
} WHERE {
	FILTER NOT EXISTS { $origin a calli:Origin }
	BIND (replace(replace(str($origin), "^[a-z]*://", ""), "/$", "") AS ?label)
};

INSERT {
    $origin calli:allowOrigin ?allowed
} WHERE {
    </> calli:allowOrigin ?allowed
	FILTER NOT EXISTS { $origin calli:allowOrigin ?allowed }
};

INSERT {
    ?realm calli:allowOrigin ?allowed
} WHERE {
    {
        ?realm a <types/Origin>
    } UNION {
        ?realm a <types/Realm>
    }
	BIND (replace(str($origin), "/$", "") AS ?allowed)
	FILTER NOT EXISTS { ?realm calli:allowOrigin ?allowed }
};

INSERT {
	$origin calli:authentication ?digest, ?yahoo, ?google;
		calli:hasComponent ?auth .
	?auth a <types/Folder>, calli:Folder;
	    rdfs:label "auth";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
	    calli:administrator </auth/groups/admin>;
	    calli:hasComponent ?digest, ?yahoo, ?google .
	?digest a <types/DigestManager>, calli:DigestManager, calli:AuthenticationManager;
		rdfs:label "Digest accounts";
		rdfs:comment "Sign in with your email address and a site password";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
		calli:administrator </auth/groups/admin>;
		calli:authButton <images/digest_64.png>;
		calli:authName ?name;
		calli:authNamespace ?space .
	?yahoo a <types/OpenIDManager>, calli:OpenIDManager, calli:AuthenticationManager;
		rdfs:label "Yahoo! accounts";
		rdfs:comment "Sign in with your Yahoo! account";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
		calli:administrator </auth/groups/admin>;
		calli:authButton <images/yahoo_64.png>;
		calli:openIdEndpointUrl "https://open.login.yahooapis.com/openid/op/auth";
		calli:openIdRealm ?realmPattern .
	?google a <types/OpenIDManager>, calli:OpenIDManager, calli:AuthenticationManager;
		rdfs:label "Google accounts";
		rdfs:comment "Sign in with your Google account";
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/everyone>;
		calli:administrator </auth/groups/admin>;
		calli:authButton <images/google_64.png>;
		calli:openIdEndpointUrl "https://www.google.com/accounts/o8/ud";
		calli:openIdRealm ?realmPattern .
} WHERE {
	{
		</> calli:authentication [calli:authName ?name; calli:authNamespace ?space]
	} UNION {
		FILTER NOT EXISTS { </> calli:authentication [calli:authName ?n; calli:authNamespace ?s] }
		BIND (replace(replace(str(</>), "^[a-z]*://", ""), "[:/].*", "") AS ?name)
		BIND (</auth/digest-users/> AS ?space)
	}
	BIND (iri(concat(str($origin),"auth/")) AS ?auth)
	BIND (iri(concat(str($origin),"auth/digest+accounts")) AS ?digest)
	BIND (iri(concat(str($origin),"auth/yahoo+accounts")) AS ?yahoo)
	BIND (iri(concat(str($origin),"auth/google+accounts")) AS ?google)
	BIND (str($origin) AS ?realmPattern)
	FILTER NOT EXISTS { $origin calli:hasComponent ?auth }
};

INSERT {
$origin calli:hasComponent ?wellknown.
?wellknown a <types/Folder>, calli:Folder;
    rdfs:label ".well known";
    calli:reader </auth/groups/public>;
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent ?void.

?void a <types/Serviceable>, void:DatasetDescription;
    rdfs:label "void";
    foaf:primaryTopic ?dataset;
    calli:reader </auth/groups/public>.

?dataset a void:Dataset;
    foaf:homepage $origin;
    void:sparqlEndpoint </sparql>;
    void:uriLookupEndpoint </sparql?uri=>;
    void:uriSpace $origin;
    void:rootResource $origin;
    void:openSearchDescription ?search.
} WHERE {
    BIND (iri(concat(str($origin),".well-known/")) AS ?wellknown)
    BIND (iri(concat(str($origin),".well-known/void")) AS ?void)
    BIND (iri(concat(str($origin),".well-known/void#dataset")) AS ?dataset)
    BIND (iri(concat(str($origin),"?search")) AS ?search)
	FILTER NOT EXISTS { $origin calli:hasComponent ?wellknown }
};
