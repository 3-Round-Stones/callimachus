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
    ?folder calli:hasComponent ?purl
} WHERE {
    ?purl a calli:Purl
    FILTER NOT EXISTS { ?container calli:hasComponent ?purl }
    BIND (iri(replace(str(?purl), "/[^/]*$", "/")) AS ?folder)
};

DELETE {
    ?purl calli:alternate ?previous
} INSERT {
    ?purl calli:alternate ?currently
} WHERE {
    <../> calli:hasComponent ?purl .
    ?purl calli:alternate ?previous .
    FILTER strstarts(str(?previous), str(</callimachus/1.0/>))
    BIND (concat(str(</callimachus/1.3/>), strafter(str(?previous),str(</callimachus/1.0/>))) AS ?currently)
};

INSERT {
    </> calli:hasComponent </admin> .
    </admin> a <types/Purl>, calli:Purl ;
    rdfs:label "admin";
    rdfs:comment "Use the link below to manage groups and invite new users";
    calli:alternate <pages/invite-users.xhtml?view>;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/admin> .
} WHERE {
	FILTER NOT EXISTS { </admin> a calli:Purl }
};

DELETE {
    ?fb a </callimachus/1.0/types/FacebookManager>;
    calli:authButton </callimachus/1.0/images/facebook_64.png>
} INSERT {
    ?fb a </callimachus/1.3/types/FacebookManager>;
    calli:authButton </callimachus/1.3/images/facebook_64.png>
} WHERE {
    ?fb a </callimachus/1.0/types/FacebookManager>;
    calli:authButton </callimachus/1.0/images/facebook_64.png>
};

INSERT {
<../> calli:hasComponent <../json-editor.html> .
<../json-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "json-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#json>) AS ?alternate)
	FILTER NOT EXISTS { <../json-editor.html> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../digest_64.png> .
<../digest_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "digest_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/digest_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../digest_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../google_64.png> .
<../google_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "google_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/google_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../google_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../yahoo_64.png> .
<../yahoo_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "yahoo_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/yahoo_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../yahoo_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../facebook_64.png> .
<../facebook_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "facebook_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/facebook_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../facebook_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../openid_64.png> .
<../openid_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "openid_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/openid_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../openid_64.png> a calli:Purl }
};

DELETE {
    ?mgr calli:authButton <images/digest_64.png>
} INSERT {
    ?mgr calli:authButton <../digest_64.png>
} WHERE {
    ?mgr calli:authButton <images/digest_64.png>
};

DELETE {
    ?mgr calli:authButton <images/google_64.png>
} INSERT {
    ?mgr calli:authButton <../google_64.png>
} WHERE {
    ?mgr calli:authButton <images/google_64.png>
};

DELETE {
    ?mgr calli:authButton <images/yahoo_64.png>
} INSERT {
    ?mgr calli:authButton <../yahoo_64.png>
} WHERE {
    ?mgr calli:authButton <images/yahoo_64.png>
};

DELETE {
    ?mgr calli:authButton <images/facebook_64.png>
} INSERT {
    ?mgr calli:authButton <../facebook_64.png>
} WHERE {
    ?mgr calli:authButton <images/facebook_64.png>
};

DELETE {
    ?mgr calli:authButton <images/openid_64.png>
} INSERT {
    ?mgr calli:authButton <../openid_64.png>
} WHERE {
    ?mgr calli:authButton <images/openid_64.png>
};

DELETE {
    ?purl calli:alternate ?iri
} INSERT {
    ?purl calli:alternate ?str
} WHERE {
    <../> calli:hasComponent ?purl .
    ?purl calli:alternate ?iri .
    FILTER isIRI(?iri)
    BIND (str(?iri) AS ?str)
};

