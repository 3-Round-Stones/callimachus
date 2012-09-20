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
    GRAPH ?graph { ?graph a </callimachus/SchemaGraph> }
} WHERE {
    GRAPH ?graph { ?graph a </callimachus/SchemaGraph> }
    FILTER NOT EXISTS {
        GRAPH ?graph { ?subject ?predicate ?object }
        FILTER (?object != </callimachus/SchemaGraph>)
    }
};

DELETE {
    ?resource calli:reader ?reader
} INSERT {
    ?resource calli:subscriber ?subscriber
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

    </group/everyone> a calli:Party, calli:Group, </callimachus/Group>;
        rdfs:label "everyone";
        rdfs:comment "A virtual group of all authorized users";
        calli:subscriber </group/staff>;
        calli:administrator </group/admin>;
        calli:everyoneFrom ".".

    </group/system> a calli:Party, calli:Group, </callimachus/Group>;
        rdfs:label "system";
        rdfs:comment "The local computer or computer systems is the member of this group";
        calli:subscriber </group/staff>;
        calli:administrator </group/admin>;
        calli:anonymousFrom "localhost".

    </group/public> a calli:Party, calli:Group, </callimachus/Group>;
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
    </.well-known/void> a </callimachus/Servicable>; calli:reader </group/public> .
} WHERE {
    FILTER NOT EXISTS { </.well-known/> calli:reader ?group }
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
    </callimachus/> calli:reader </group/public>
} WHERE {
    FILTER NOT EXISTS { </callimachus/> calli:reader ?group }
};

INSERT {
    ?resource calli:reader </group/public>
} WHERE {
    {
        ?resource a </callimachus/AnimatedGraphic>
    } UNION {
        ?resource a </callimachus/Article>
    } UNION {
        ?resource a </callimachus/Concept>
    } UNION {
        ?resource a </callimachus/Folder>
    } UNION {
        ?resource a </callimachus/Font>
    } UNION {
        ?resource a </callimachus/GraphDocument>
    } UNION {
        ?resource a </callimachus/HypertextFile>
    } UNION {
        ?resource a </callimachus/IconGraphic>
    } UNION {
        ?resource a </callimachus/NamedGraph>
    } UNION {
        ?resource a </callimachus/NamedQuery>
    } UNION {
        ?resource a </callimachus/NetworkGraphic>
    } UNION {
        ?resource a </callimachus/Origin>
    } UNION {
        ?resource a </callimachus/Page>
    } UNION {
        ?resource a </callimachus/Pdf>
    } UNION {
        ?resource a </callimachus/Pipeline>
    } UNION {
        ?resource a </callimachus/PURL>
    } UNION {
        ?resource a </callimachus/Realm>
    } UNION {
        ?resource a </callimachus/Relax>
    } UNION {
        ?resource a </callimachus/Schema>
    } UNION {
        ?resource a </callimachus/Schematron>
    } UNION {
        ?resource a </callimachus/Script>
    } UNION {
        ?resource a </callimachus/Style>
    } UNION {
        ?resource a </callimachus/TextFile>
    } UNION {
        ?resource a </callimachus/Theme>
    } UNION {
        ?resource a </callimachus/Transform>
    } UNION {
        ?resource a </callimachus/VectorGraphic>
    }
    FILTER (?resource != </activity/>)
    FILTER (?resource != </callimachus/>)
    FILTER (?resource != </group/>)
    FILTER (?resource != </user/>)
    FILTER NOT EXISTS { ?resource calli:reader ?group }
};

INSERT {
    ?class calli:reader </group/public>
} WHERE {
    ?class a </callimachus/Class>
    FILTER NOT EXISTS { ?class calli:reader ?group }
    FILTER NOT EXISTS { ?class calli:realm ?realm }
};

