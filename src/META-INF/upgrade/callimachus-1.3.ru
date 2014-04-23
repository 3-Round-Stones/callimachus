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

