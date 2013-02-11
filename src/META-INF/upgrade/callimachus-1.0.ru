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

DELETE {
    ?folder calli:describedby ?describedby
} WHERE {
    ?folder a <types/Folder>; calli:describedby ?describedby
    FILTER EXISTS {
        ?folder calli:describedby ?describedby2
        FILTER (?describedby2 < ?describedby)
    }
};

INSERT {
    </> calli:reader </auth/groups/public>
} WHERE {
    FILTER NOT EXISTS {
        </> calli:reader </auth/groups/public>
    }
};

DELETE {
    </layout/template.xsl> ?p ?o
} WHERE {
    </layout/template.xsl> ?p ?o
};

DELETE {
    </> a <http://callimachusproject.org/rdf/2009/framework#Alias>
} WHERE {
    </> a <http://callimachusproject.org/rdf/2009/framework#Alias>, calli:Folder
};

INSERT {
    ?graph a <http://www.openrdf.org/rdf/2012/auditing#ObsoleteBundle>
} WHERE {
    ?graph a <http://www.openrdf.org/rdf/2009/auditing#ObsoleteTransaction>
    FILTER NOT EXISTS { ?graph a <http://www.openrdf.org/rdf/2012/auditing#ObsoleteBundle> }
};

INSERT {
<../> calli:hasComponent <../document-editor.html>.
<../document-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "document-editor.html";
	calli:alternate <editor/ckeditor.html>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../document-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../css-editor.html>.
<../css-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "css-editor.html";
	calli:alternate <editor/text-editor.html#css>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../css-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../html-editor.html>.
<../html-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "html-editor.html";
	calli:alternate <editor/text-editor.html#html>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../html-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../javascript-editor.html>.
<../javascript-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "javascript-editor.html";
	calli:alternate <editor/text-editor.html#javascript>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../javascript-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../sparql-editor.html>.
<../sparql-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "sparql-editor.html";
	calli:alternate <editor/text-editor.html#sparql>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../sparql-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../text-editor.html>.
<../text-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "text-editor.html";
	calli:alternate <editor/text-editor.html>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../css-editor.html> a calli:PURL }
};

INSERT {
<../> calli:hasComponent <../xml-editor.html>.
<../xml-editor.html> a <types/PURL>, calli:PURL ;
	rdfs:label "xml-editor.html";
	calli:alternate <editor/text-editor.html#xml>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../xml-editor.html> a calli:PURL }
};

DELETE {
    GRAPH ?graph {
	    ?reader calli:reader ?group .
	    ?author calli:author ?group .
	    ?editor calli:editor ?group .
	    ?administrator calli:administrator ?group .
	    ?subscriber calli:subscriber ?group .
	    ?contributor calli:contributor ?group .
	}
} INSERT {
    GRAPH ?graph {
	    ?reader calli:reader ?auth .
	    ?author calli:author ?auth .
	    ?editor calli:editor ?auth .
	    ?administrator calli:administrator ?auth .
	    ?subscriber calli:subscriber ?auth .
	    ?contributor calli:contributor ?auth .
	}
} WHERE {
	</group/> calli:hasComponent ?group .
	</auth/groups/> calli:hasComponent ?auth .
	FILTER (strafter(str(?group),str(</group/>)) = strafter(str(?auth),str(</auth/groups/>))
	GRAPH ?graph {
	    {
		    ?reader calli:reader ?group
	    } UNION {
		    ?author calli:author ?group
	    } UNION {
		    ?editor calli:editor ?group
	    } UNION {
		    ?administrator calli:administrator ?group
	    } UNION {
		    ?subscriber calli:subscriber ?group
	    } UNION {
		    ?contributor calli:contributor ?group
	    }
	}
};

DELETE {
	</group/> calli:hasComponent ?group .
	?group ?pred ?obj .
} WHERE {
	</group/> calli:hasComponent ?group .
	?group ?pred ?obj .
	</auth/groups/> calli:hasComponent ?auth .
	FILTER (strafter(str(?group),str(</group/>)) = strafter(str(?auth),str(</auth/groups/>))
};

DELETE {
    ?resource a ?previous .
} INSERT {
	?resource a ?current .
} WHERE {
{ ?resource a </callimachus/Class> BIND (</callimachus/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ ?resource a </callimachus/Composite> BIND (</callimachus/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ ?resource a </callimachus/Concept> BIND (</callimachus/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ ?resource a </callimachus/Creatable> BIND (</callimachus/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ ?resource a </callimachus/Editable> BIND (</callimachus/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ ?resource a </callimachus/Serviceable> BIND (</callimachus/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ ?resource a </callimachus/Viewable> BIND (</callimachus/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) }
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?previous }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?current }
} WHERE {
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Class> } BIND (</callimachus/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Composite> } BIND (</callimachus/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Concept> } BIND (</callimachus/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Creatable> } BIND (</callimachus/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Editable> } BIND (</callimachus/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Serviceable> } BIND (</callimachus/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Viewable> } BIND (</callimachus/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) }
};

DELETE {
	</callimachus/ontology> owl:versionInfo "1.0"
} INSERT {
	</callimachus/ontology> owl:versionInfo "1.0.1"
} WHERE {
	</callimachus/ontology> owl:versionInfo "1.0"
};

