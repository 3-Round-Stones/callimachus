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
    ?folder calli:describedby ?resource
} INSERT {
    ?folder calli:describedby ?content
} WHERE {
    ?folder calli:describedby ?resource
    FILTER isIRI(?resource)
    FILTER strends(str(?folder),"/")
    BIND (if(strstarts(str(?resource),str(?folder)),strafter(str(?resource),str(?folder)),str(?resource)) AS ?content)
};

DELETE {
    ?folder calli:describedBy ?resource
} INSERT {
    ?folder calli:describedby ?content
} WHERE {
    ?folder calli:describedBy ?resource
    FILTER isIRI(?resource)
    FILTER strends(str(?folder),"/")
    BIND (if(strstarts(str(?resource),str(?folder)),strafter(str(?resource),str(?folder)),str(?resource)) AS ?content)
};

DELETE {
    GRAPH ?graph { ?graph a <SchemaGraph> }
} WHERE {
    GRAPH ?graph { ?graph a <SchemaGraph> }
    FILTER NOT EXISTS {
        GRAPH ?graph { ?subject ?predicate ?object }
        FILTER (?object != <SchemaGraph>)
    }
};

DELETE {
    ?resource calli:reader ?reader
} INSERT {
    ?resource calli:subscriber ?reader
} WHERE {
    ?resource calli:reader ?reader
    FILTER NOT EXISTS { ?any calli:subscriber ?subscriber }
};

INSERT {
    ?group calli:membersFrom "."
} WHERE {
    ?group a calli:Group
    FILTER NOT EXISTS { ?group calli:anonymousFrom ?somewhere }
    FILTER NOT EXISTS { ?group calli:everyoneFrom ?somewhere }
    FILTER NOT EXISTS { ?group calli:membersFrom ?somewhere }
};

INSERT {
    </group/> calli:hasComponent </group/everyone>, </group/system>, </group/public>.

    </group/everyone> a calli:Party, calli:Group, <Group>;
        rdfs:label "everyone";
        rdfs:comment "A virtual group of all authorized users";
        calli:subscriber </group/staff>;
        calli:administrator </group/admin>;
        calli:everyoneFrom ".".

    </group/system> a calli:Party, calli:Group, <Group>;
        rdfs:label "system";
        rdfs:comment "The local computer or computer systems is the member of this group";
        calli:subscriber </group/staff>;
        calli:administrator </group/admin>.

    </group/public> a calli:Party, calli:Group, <Group>;
        rdfs:label "public";
        rdfs:comment "A virtual group of all agents";
        calli:subscriber </group/staff>;
        calli:administrator </group/admin>;
        calli:anonymousFrom ".".
} WHERE {
    FILTER NOT EXISTS { </group/public> a calli:Group }
};

INSERT {
    </accounts> calli:reader </group/public>; calli:subscriber </group/users>, </group/staff>;
} WHERE {
    FILTER NOT EXISTS { </accounts> calli:reader ?group }
};

INSERT {
    </.well-known/> calli:reader </group/public> .
    </.well-known/void> a <Serviceable>; calli:reader </group/public> .
} WHERE {
    FILTER NOT EXISTS { </.well-known/> calli:reader ?group }
};

DELETE {
    </.well-known/void> a <Servicable>
} INSERT {
    </.well-known/void> a <Serviceable>
} WHERE {
    </.well-known/void> a <Servicable>
};

INSERT {
    </callimachus> calli:administrator </group/admin>
} WHERE {
    FILTER NOT EXISTS { </callimachus> calli:administrator ?group }
};

INSERT {
    </main+menu> calli:reader </group/public>
} WHERE {
    FILTER NOT EXISTS { </main+menu> calli:reader ?group }
};

INSERT {
    <> calli:reader </group/public>
} WHERE {
    FILTER NOT EXISTS { <> calli:reader ?group }
};

INSERT {
    ?resource calli:reader </group/public>
} WHERE {
    {
        ?resource a <AnimatedGraphic>
    } UNION {
        ?resource a <Article>
    } UNION {
        ?resource a <Concept>
    } UNION {
        ?resource a <Folder>
    } UNION {
        ?resource a <Font>
    } UNION {
        ?resource a <GraphDocument>
    } UNION {
        ?resource a <HypertextFile>
    } UNION {
        ?resource a <IconGraphic>
    } UNION {
        ?resource a <NamedGraph>
    } UNION {
        ?resource a <NamedQuery>
    } UNION {
        ?resource a <NetworkGraphic>
    } UNION {
        ?resource a <Origin>
    } UNION {
        ?resource a <Page>
    } UNION {
        ?resource a <Pdf>
    } UNION {
        ?resource a <Pipeline>
    } UNION {
        ?resource a <PURL>
    } UNION {
        ?resource a <Realm>
    } UNION {
        ?resource a <Relax>
    } UNION {
        ?resource a <Schema>
    } UNION {
        ?resource a <Schematron>
    } UNION {
        ?resource a <Script>
    } UNION {
        ?resource a <Style>
    } UNION {
        ?resource a <TextFile>
    } UNION {
        ?resource a <Theme>
    } UNION {
        ?resource a <Transform>
    } UNION {
        ?resource a <VectorGraphic>
    }
    FILTER (?resource != </activity/>)
    FILTER (?resource != <>)
    FILTER (?resource != </group/>)
    FILTER (?resource != </user/>)
    FILTER NOT EXISTS { ?resource calli:reader ?group }
};

INSERT {
    ?class calli:reader </group/public>
} WHERE {
    ?class a <Class>
    FILTER NOT EXISTS { ?class calli:reader ?group }
    FILTER NOT EXISTS { ?class calli:realm ?realm }
};

DELETE {
    ?realm calli:forbidden <pages/forbidden.xhtml>
} INSERT {
    ?realm calli:forbidden <pages/forbidden.xhtml?element=/1&realm=/>
} WHERE {
    ?realm calli:forbidden <pages/forbidden.xhtml>
};

DELETE {
    ?realm calli:unauthorized <pages/unauthorized.xhtml?element=/1&realm=/>
} INSERT {
    ?realm calli:unauthorized <pages/unauthorized.xhtml?element=/1>
} WHERE {
    ?realm calli:unauthorized <pages/unauthorized.xhtml?element=/1&realm=/>
};

DELETE {
    ?realm calli:unauthorized <pages/unauthorized.xhtml>
} INSERT {
    ?realm calli:unauthorized <pages/unauthorized.xhtml?element=/1>
} WHERE {
    ?realm calli:unauthorized <pages/unauthorized.xhtml>
};

DELETE {
    ?graph a <SchemaGraph>
    GRAPH ?graph { <Serviceable> owl:equivalentClass rdfs:Resource }
} WHERE {
    ?graph a <SchemaGraph>
    GRAPH ?graph { <Serviceable> owl:equivalentClass rdfs:Resource }
    FILTER NOT EXISTS { GRAPH ?graph {
        ?s ?p ?o
        FILTER (<Serviceable> != ?s || owl:equivalentClass != ?p || rdfs:Resource != ?o)
    }}
};

INSERT {
    ?accounts a calli:AuthenticationManager
} WHERE {
    ?accounts a calli:DigestManager
    FILTER NOT EXISTS { ?accounts a calli:AuthenticationManager }
};

INSERT {
    </callimachus> a <Serviceable>
} WHERE {
    </callimachus> a owl:Ontology
    FILTER NOT EXISTS { </callimachus> a <Serviceable> }
};

DELETE {
    ?digest calli:authName ?iri
} INSERT {
    ?digest calli:authName ?authName
} WHERE {
    ?digest a <DigestManager>; calli:authName ?iri
    BIND (str(?iri) AS ?authName)
    FILTER (?iri != ?authName)
};

INSERT {
    ?resource calli:administrator </group/admin>
} WHERE {
    <> calli:hasComponent* ?resource
    FILTER NOT EXISTS { ?resource calli:administrator ?admin }
};

DELETE {
	</callimachus> owl:versionInfo "0.17"
} INSERT {
	</callimachus> owl:versionInfo "0.18"
} WHERE {
	</callimachus> owl:versionInfo "0.17"
};

