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
</auth/> calli:hasComponent </auth/secrets/>.
</auth/secrets/> a <types/Folder>, calli:Folder;
    rdfs:label "secrets".
} WHERE {
	FILTER NOT EXISTS { </auth/secrets/> a calli:Folder }
};

INSERT {
</auth/groups/> calli:hasComponent </auth/groups/super>.
</auth/groups/super> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "super";
    rdfs:comment "The user accounts in this group have heightened privileges to change or patch the system itself".
} WHERE {
	FILTER NOT EXISTS { </auth/groups/super> a calli:Group }
};

INSERT {
</auth/groups/> calli:hasComponent </auth/groups/power>.
</auth/groups/power> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "power";
    rdfs:comment "Members of this group can access all data in the underlying data store";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:membersFrom ".".
} WHERE {
	FILTER NOT EXISTS { </auth/groups/power> a calli:Group }
};

INSERT {
    ?user a <types/DigestUser>
} WHERE {
    ?user a <types/User>
};

INSERT {
</auth/> calli:hasComponent </auth/invited-users/>.
</auth/invited-users/> a <types/Folder>, calli:Folder;
    rdfs:label "invited users";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/invited-users/> a calli:Folder }
};

INSERT {
    </sparql> calli:reader </auth/groups/power>;
} WHERE {
    </sparql> a <types/SparqlService>, sd:Service .
    FILTER NOT EXISTS { </sparql> calli:reader </auth/groups/power> }
};

INSERT {
	?file calli:reader </auth/groups/admin>
} WHERE {
	{
		<types/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/system>
	} UNION {
		<pipelines/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/system>
	} UNION {
		<queries/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/system>
	} UNION {
		<schemas/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/system>
	} UNION {
		<transforms/> calli:hasComponent? ?file . ?file calli:reader </auth/groups/system>
	}
	FILTER NOT EXISTS { ?file calli:reader </auth/groups/admin> }
};

INSERT {
    ?digest calli:authName ?authNameLit
} WHERE {
    ?digest a calli:DigestManager; calli:authName ?authName
    FILTER NOT EXISTS { ?digest calli:authName ?lit FILTER isLiteral(?lit) }
    FILTER isIRI(?authName)
    BIND (str(?authName) as ?authNameLit)
};

DELETE {
    ?digest calli:authName ?authName
} WHERE {
    ?digest a calli:DigestManager; calli:authName ?authName
    FILTER isIRI(?authName)
};

