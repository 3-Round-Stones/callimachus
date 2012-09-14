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
