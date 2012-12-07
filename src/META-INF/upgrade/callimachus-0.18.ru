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
    ?realm calli:menu ?menu .
	?realm calli:favicon ?favicon .
	?realm calli:variation ?variation .
	?realm calli:rights ?rights .
} WHERE {
    {
        ?realm calli:menu ?menu .
    } UNION {
	    ?realm calli:favicon ?favicon .
    } UNION {
	    ?realm calli:variation ?variation .
	} UNION {
	    ?realm calli:rights ?rights .
	}
};

DELETE {
    ?composite calli:hasComponent ?menu .
    ?menu a calli:Menu .
    ?menu a </callimachus/Menu> .
    ?menu a </callimachus/types/Menu> .
    ?menu a </callimachus/0.18/types/Menu> .
    ?menu rdfs:label ?label .
    ?menu calli:reader ?reader .
    ?menu calli:subscriber ?subscriber .
    ?menu calli:editor ?editor .
    ?menu calli:administrator ?administrator .
    ?menu calli:link ?link .
    ?menu calli:item ?item .
    ?menu calli:position ?position .
} WHERE {
    {
        ?menu a calli:Menu .
    } UNION {
        ?parent calli:item ?menu .
    }
    {
        ?composite calli:hasComponent ?menu .
    } UNION {
        ?menu a </callimachus/Menu> .
    } UNION {
        ?menu a </callimachus/types/Menu> .
    } UNION {
        ?menu a </callimachus/0.18/types/Menu> .
    } UNION {
        ?menu rdfs:label ?label .
    } UNION {
        ?menu calli:reader ?reader .
    } UNION {
        ?menu calli:subscriber ?subscriber .
    } UNION {
        ?menu calli:editor ?editor .
    } UNION {
        ?menu calli:administrator ?administrator .
    } UNION {
        ?menu calli:link ?link .
    } UNION {
        ?menu calli:item ?item .
    } UNION {
        ?menu calli:position ?position .
    }
};

DELETE {
</> calli:hasComponent </callimachus>.
</callimachus> a ?ontology;
    rdfs:label ?label;
    rdfs:comment ?comment;
    owl:versionInfo ?version;
    calli:administrator ?group
} INSERT {
</callimachus/> calli:hasComponent </callimachus/ontology>.
</callimachus/ontology> a </callimachus/1.0/types/Serviceable>, owl:Ontology;
    rdfs:label "ontology";
    rdfs:comment "Vocabulary used to create local Callimachus applications";
    owl:versionInfo "0.18";
    calli:administrator </auth/groups/admin>.
} WHERE {
</> calli:hasComponent </callimachus>.
</callimachus> a ?ontology;
    rdfs:label ?label;
    rdfs:comment ?comment;
    owl:versionInfo ?version;
    calli:administrator ?group
};

DELETE {
    ?accounts calli:authNamespace </auth/passwords/>;
} INSERT {
    ?accounts calli:authNamespace </auth/digest-users/>;
} WHERE {
    ?accounts calli:authNamespace </auth/passwords/>;
};

DELETE {
	?realm calli:authentication </accounts> .
	</> calli:hasComponent </accounts> .
	</auth/digest+account> calli:authNamespace </user/> .
	</accounts>
		calli:administrator ?admin;
		calli:authName ?authName ;
		calli:authNamespace </user/>;
		calli:reader ?reader;
		calli:subscriber ?subscriber;
		prov:wasGeneratedBy ?provenance;
		rdf:type ?type;
		rdfs:label ?label .
} INSERT {
	?realm calli:authentication </auth/digest+account> .
	</auth/> calli:hasComponent </auth/digest+account> .
	</auth/digest+account>
		calli:administrator </auth/groups/admin>;
		calli:authName ?authName ;
		calli:authNamespace </auth/digest-users/>;
		calli:reader </auth/groups/public>;
		calli:subscriber </auth/groups/staff>;
		calli:subscriber </auth/groups/users>;
		rdf:type calli:AuthenticationManager;
		rdf:type <types/DigestManager>;
		rdf:type calli:DigestManager;
		rdfs:label "Digest account" .
} WHERE {
	?realm calli:authentication </accounts> .
	</> calli:hasComponent </accounts> .
	</accounts>
		calli:administrator ?admin;
		calli:authName ?authName ;
		calli:authNamespace </user/>;
		calli:reader ?reader;
		calli:subscriber ?subscriber;
		prov:wasGeneratedBy ?provenance;
		rdf:type ?type;
		rdfs:label ?label .
};

INSERT {
</> calli:hasComponent </auth/>.
</auth/> a <types/Folder>, calli:Folder;
    rdfs:label "auth";
	calli:reader </auth/groups/public>;
	calli:subscriber </auth/groups/users>, </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent </auth/groups/>, </auth/digest-users/>.
} WHERE {
	FILTER NOT EXISTS { </auth/> a calli:Folder }
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
	BIND (iri(concat(str(</auth/groups/>),strafter(str(?group),str(</group/>)))) AS ?auth)
};

DELETE {
	</> calli:hasComponent </group/>.
	</group/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} INSERT {
	</auth/> calli:hasComponent </auth/groups/>.
	</auth/groups/> a <types/Folder>, calli:Folder;
	    rdfs:label "groups";
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} WHERE {
	</> calli:hasComponent </group/>.
	</group/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
};

DELETE {
	</group/> calli:hasComponent ?group .
	?group ?pred ?obj .
} INSERT {
	</auth/groups/> calli:hasComponent ?auth .
	?auth ?pred ?obj .
} WHERE {
	</group/> calli:hasComponent ?group .
	?group ?pred ?obj .
	BIND (iri(concat(str(</auth/groups/>),strafter(str(?group),str(</group/>)))) AS ?auth)
};

DELETE {
    GRAPH ?graph {
	    ?reader calli:reader ?user .
	    ?author calli:author ?user .
	    ?editor calli:editor ?user .
	    ?administrator calli:administrator ?user .
	    ?subscriber calli:subscriber ?user .
	    ?contributor calli:contributor ?user .
	    ?member calli:member ?user .
	}
} INSERT {
    GRAPH ?graph {
	    ?reader calli:reader ?auth .
	    ?author calli:author ?auth .
	    ?editor calli:editor ?auth .
	    ?administrator calli:administrator ?auth .
	    ?subscriber calli:subscriber ?auth .
	    ?contributor calli:contributor ?auth .
	    ?member calli:member ?auth .
	}
} WHERE {
	</auth/passwords/> calli:hasComponent ?user .
    GRAPH ?graph {
	    {
		    ?reader calli:reader ?user
	    } UNION {
		    ?author calli:author ?user
	    } UNION {
		    ?editor calli:editor ?user
	    } UNION {
		    ?administrator calli:administrator ?user
	    } UNION {
		    ?subscriber calli:subscriber ?user
	    } UNION {
		    ?contributor calli:contributor ?user
	    } UNION {
		    ?member calli:member ?user .
	    }
	}
	BIND (iri(concat(str(</auth/digest-users/>),strafter(str(?user),str(</auth/passwords/>)))) AS ?auth)
};

DELETE {
    GRAPH ?graph {
	    ?reader calli:reader ?user .
	    ?author calli:author ?user .
	    ?editor calli:editor ?user .
	    ?administrator calli:administrator ?user .
	    ?subscriber calli:subscriber ?user .
	    ?contributor calli:contributor ?user .
	    ?member calli:member ?user .
	}
} INSERT {
    GRAPH ?graph {
	    ?reader calli:reader ?auth .
	    ?author calli:author ?auth .
	    ?editor calli:editor ?auth .
	    ?administrator calli:administrator ?auth .
	    ?subscriber calli:subscriber ?auth .
	    ?contributor calli:contributor ?auth .
	    ?member calli:member ?auth .
	}
} WHERE {
	</user/> calli:hasComponent ?user .
    GRAPH ?graph {
	    {
		    ?reader calli:reader ?user
	    } UNION {
		    ?author calli:author ?user
	    } UNION {
		    ?editor calli:editor ?user
	    } UNION {
		    ?administrator calli:administrator ?user
	    } UNION {
		    ?subscriber calli:subscriber ?user
	    } UNION {
		    ?contributor calli:contributor ?user
	    } UNION {
		    ?member calli:member ?user .
	    }
	}
	BIND (iri(concat(str(</auth/digest-users/>),strafter(str(?user),str(</user/>)))) AS ?auth)
};

DELETE {
	</> calli:hasComponent </auth/passwords/>.
	</auth/passwords/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} INSERT {
	</auth/> calli:hasComponent </auth/digest-users/>.
	</auth/digest-users/> a <types/Folder>, calli:Folder;
	    rdfs:label "digest users";
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} WHERE {
	</> calli:hasComponent </auth/passwords/>.
	</auth/passwords/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
};

DELETE {
	</auth/passwords/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} INSERT {
	</auth/digest-users/> a <types/Folder>, calli:Folder;
	    rdfs:label "digest users";
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} WHERE {
	</auth/passwords/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
};

DELETE {
	</> calli:hasComponent </user/>.
	</user/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} INSERT {
	</auth/> calli:hasComponent </auth/digest-users/>.
	</auth/digest-users/> a <types/Folder>, calli:Folder;
	    rdfs:label "digest users";
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
} WHERE {
	</> calli:hasComponent </user/>.
	</user/> a ?type;
	    rdfs:label ?label;
	    calli:subscriber ?subscriber;
	    calli:administrator ?admin;
};

DELETE {
	</auth/passwords/> calli:hasComponent ?user .
	?user ?pred ?obj .
} INSERT {
	</auth/digest-users/> calli:hasComponent ?auth .
	?auth ?pred ?obj .
} WHERE {
	</auth/passwords/> calli:hasComponent ?user .
	?user ?pred ?obj .
	BIND (iri(concat(str(</auth/digest-users/>),strafter(str(?user),str(</auth/passwords/>)))) AS ?auth)
};

DELETE {
	</user/> calli:hasComponent ?user .
	?user ?pred ?obj .
} INSERT {
	</auth/digest-users/> calli:hasComponent ?auth .
	?auth ?pred ?obj .
} WHERE {
	</user/> calli:hasComponent ?user .
	?user ?pred ?obj .
	BIND (iri(concat(str(</auth/digest-users/>),strafter(str(?user),str(</user/>)))) AS ?auth)
};

INSERT {
	GRAPH ?obsolete { ?obsolete a audit:ObsoleteBundle }
} WHERE {
	{
	    ?obsolete a <http://www.openrdf.org/rdf/2009/auditing#Transaction>
	} UNION {
	    ?obsolete a </callimachus/Activity>
	} UNION {
	    ?obsolete a </callimachus/types/Activity>
	} UNION {
	    ?obsolete a </callimachus/0.18/types/Activity>
	} UNION {
	    ?obsolete a </callimachus/0.18/types/Change>
	}
	FILTER NOT EXISTS {
		GRAPH ?obsolete {
			?s ?p ?o
			FILTER ( !strstarts(str(?s),str(?obsolete)) )
			FILTER ( !strstarts(str(?p),str(rdf:)) || sameTerm(?p,rdf:type) && !strstarts(str(?o),"http://www.openrdf.org/rdf/2009/auditing#") )
			FILTER ( !strstarts(str(?p),str(audit:)) )
			FILTER ( !strstarts(str(?p),"http://www.openrdf.org/rdf/2009/auditing#") )
			FILTER ( !strstarts(str(?p),"http://www.openrdf.org/rdf/2011/keyword#") )
			FILTER ( !strstarts(str(?p),str(prov:)) || sameTerm(?p,prov:wasGeneratedBy) )
		}
	}
	FILTER EXISTS { GRAPH ?obsolete { ?s ?p ?o } }
};

DELETE {
    </callimachus/library.xpl> calli:alternate </callimachus/0.18/library.xpl> .
} INSERT {
    </callimachus/library.xpl> calli:alternate </callimachus/1.0/library.xpl> .
} WHERE {
    </callimachus/library.xpl> calli:alternate </callimachus/0.18/library.xpl> .
};

DELETE {
    </callimachus/Concept> calli:alternate </callimachus/0.18/Concept> .
} INSERT {
    </callimachus/Concept> calli:alternate </callimachus/1.0/Concept> .
} WHERE {
    </callimachus/Concept> calli:alternate </callimachus/0.18/Concept> .
};

DELETE {
	</callimachus/> calli:hasComponent </callimachus/template.xsl>.
	</callimachus/template.xsl> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
} WHERE {
	</callimachus/> calli:hasComponent </callimachus/template.xsl>.
	</callimachus/template.xsl> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/library.xpl>.
	</callimachus/library.xpl> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label "library.xpl";
		calli:alternate </callimachus/1.0/library.xpl>;
		calli:administrator </auth/groups/admin>;
		calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/library.xpl> a calli:PURL }
};

DELETE {
	</callimachus/> calli:hasComponent </callimachus/forbidden>.
	</callimachus/forbidden> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
} WHERE {
	</callimachus/> calli:hasComponent </callimachus/forbidden>.
	</callimachus/forbidden> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
};

DELETE {
	</callimachus/> calli:hasComponent </callimachus/unauthorized>.
	</callimachus/unauthorized> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
} WHERE {
	</callimachus/> calli:hasComponent </callimachus/unauthorized>.
	</callimachus/unauthorized> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label ?label;
		calli:alternate ?alternate;
		calli:administrator ?admin;
		calli:reader ?reader .
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/forbidden.html>.
	</callimachus/forbidden.html> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label "forbidden.html";
		calli:alternate </callimachus/1.0/pages/forbidden.xhtml?element=/1&realm=/>;
		calli:administrator </auth/groups/admin>;
		calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/forbidden.html> a calli:PURL }
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/unauthorized.html>.
	</callimachus/unauthorized.html> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label "unauthorized.html";
		calli:alternate </callimachus/1.0/pages/unauthorized.xhtml?element=/1>;
		calli:administrator </auth/groups/admin>;
		calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/unauthorized.html> a calli:PURL }
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/default-layout.xq>.
    </callimachus/default-layout.xq> a </callimachus/1.0/types/PURL>, calli:PURL ;
	    rdfs:label "default-layout.xq";
	    calli:alternate </callimachus/1.0/pages/default-layout.xq>;
	    calli:administrator </auth/groups/admin>;
	    calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/default-layout.xq> a calli:PURL }
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/layout-elements.xq>.
    </callimachus/layout-elements.xq> a </callimachus/1.0/types/PURL>, calli:PURL ;
	    rdfs:label "layout-elements.xq";
	    calli:alternate </callimachus/1.0/pages/layout-elements.xq>;
	    calli:administrator </auth/groups/admin>;
	    calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/layout-elements.xq> a calli:PURL }
};

INSERT {
	</callimachus/> calli:hasComponent </callimachus/Concept>.
	</callimachus/Concept> a </callimachus/1.0/types/PURL>, calli:PURL ;
		rdfs:label "Concept";
		calli:canonical </callimachus/1.0/types/Concept>;
		calli:administrator </auth/groups/admin>;
		calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { </callimachus/Concept> a calli:PURL }
};

INSERT {
</callimachus/> calli:hasComponent </callimachus/changes/> .
</callimachus/changes/> a <types/Folder>, calli:Folder;
    rdfs:label "changes";
    calli:subscriber </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </callimachus/> calli:hasComponent </callimachus/changes/> }
};

DELETE {
	?realm calli:favicon </callimachus/images/callimachus-icon.ico>
} INSERT {
	?realm calli:favicon </favicon.ico>
} WHERE {
	?realm calli:favicon </callimachus/images/callimachus-icon.ico>
};

DELETE {
	?realm calli:forbidden </callimachus/pages/forbidden.xhtml?element=/1&realm=/>
} INSERT {
	?realm calli:forbidden </callimachus/forbidden.html>
} WHERE {
	?realm calli:forbidden </callimachus/pages/forbidden.xhtml?element=/1&realm=/>
};

DELETE {
	?realm calli:unauthorized </callimachus/pages/unauthorized.xhtml?element=/1>
} INSERT {
	?realm calli:unauthorized </callimachus/unauthorized.html>
} WHERE {
	?realm calli:unauthorized </callimachus/pages/unauthorized.xhtml?element=/1>
};

DELETE {
	?realm calli:forbidden </callimachus/forbidden>
} INSERT {
	?realm calli:forbidden </callimachus/forbidden.html>
} WHERE {
	?realm calli:forbidden </callimachus/forbidden>
};

DELETE {
	?realm calli:unauthorized </callimachus/unauthorized>
} INSERT {
	?realm calli:unauthorized </callimachus/unauthorized.html>
} WHERE {
	?realm calli:unauthorized </callimachus/unauthorized>
};

DELETE {
	?realm calli:theme ?theme
} INSERT {
	?realm calli:layout </callimachus/default-layout.xq>
} WHERE {
	?realm calli:theme ?theme
};

DELETE {
	?realm calli:layout </callimachus/layout.xq>
} INSERT {
	?realm calli:layout </callimachus/default-layout.xq>
} WHERE {
	?realm calli:layout </callimachus/layout.xq>
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Local> }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Serviceable> }
} WHERE {
	GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Local> }
};

DELETE {
	?resource a ?previous .
} INSERT {
	?resource a ?current .
} WHERE {
{ ?resource a </callimachus/Activity> BIND (</callimachus/Activity> AS ?previous) BIND (</callimachus/1.0/types/Change> AS ?current) } UNION
{ ?resource a </callimachus/Article> BIND (</callimachus/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ ?resource a </callimachus/Book> BIND (</callimachus/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ ?resource a </callimachus/Class> BIND (</callimachus/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ ?resource a </callimachus/Composite> BIND (</callimachus/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ ?resource a </callimachus/Concept> BIND (</callimachus/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ ?resource a </callimachus/Creatable> BIND (</callimachus/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ ?resource a </callimachus/DigestManager> BIND (</callimachus/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ ?resource a </callimachus/Editable> BIND (</callimachus/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ ?resource a </callimachus/FacebookManager> BIND (</callimachus/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ ?resource a </callimachus/File> BIND (</callimachus/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ ?resource a </callimachus/Folder> BIND (</callimachus/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ ?resource a </callimachus/Font> BIND (</callimachus/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ ?resource a </callimachus/SchemaGraph> BIND (</callimachus/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ ?resource a </callimachus/NamedGraph> BIND (</callimachus/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ ?resource a </callimachus/GraphDocument> BIND (</callimachus/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ ?resource a </callimachus/Group> BIND (</callimachus/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ ?resource a </callimachus/HypertextFile> BIND (</callimachus/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ ?resource a </callimachus/Image> BIND (</callimachus/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ ?resource a </callimachus/AnimatedGraphic> BIND (</callimachus/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ ?resource a </callimachus/IconGraphic> BIND (</callimachus/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ ?resource a </callimachus/NetworkGraphic> BIND (</callimachus/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ ?resource a </callimachus/VectorGraphic> BIND (</callimachus/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ ?resource a </callimachus/Origin> BIND (</callimachus/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ ?resource a </callimachus/Page> BIND (</callimachus/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ ?resource a </callimachus/Pdf> BIND (</callimachus/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ ?resource a </callimachus/Photo> BIND (</callimachus/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ ?resource a </callimachus/Pipeline> BIND (</callimachus/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ ?resource a </callimachus/Profile> BIND (</callimachus/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ ?resource a </callimachus/PURL> BIND (</callimachus/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ ?resource a </callimachus/NamedQuery> BIND (</callimachus/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ ?resource a </callimachus/Realm> BIND (</callimachus/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ ?resource a </callimachus/Relax> BIND (</callimachus/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ ?resource a </callimachus/Schematron> BIND (</callimachus/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ ?resource a </callimachus/Script> BIND (</callimachus/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ ?resource a </callimachus/Serviceable> BIND (</callimachus/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ ?resource a </callimachus/SparqlService> BIND (</callimachus/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ ?resource a </callimachus/Style> BIND (</callimachus/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ ?resource a </callimachus/TextFile> BIND (</callimachus/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ ?resource a </callimachus/Transform> BIND (</callimachus/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ ?resource a </callimachus/User> BIND (</callimachus/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ ?resource a </callimachus/Viewable> BIND (</callimachus/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ ?resource a </callimachus/XQuery> BIND (</callimachus/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?resource),str(</callimachus/>)))
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?previous }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?current }
} WHERE {
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Article> } BIND (</callimachus/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Book> } BIND (</callimachus/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Class> } BIND (</callimachus/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Composite> } BIND (</callimachus/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Concept> } BIND (</callimachus/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Creatable> } BIND (</callimachus/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/DigestManager> } BIND (</callimachus/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Editable> } BIND (</callimachus/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/FacebookManager> } BIND (</callimachus/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/File> } BIND (</callimachus/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Folder> } BIND (</callimachus/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Font> } BIND (</callimachus/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/SchemaGraph> } BIND (</callimachus/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/NamedGraph> } BIND (</callimachus/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/GraphDocument> } BIND (</callimachus/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Group> } BIND (</callimachus/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/HypertextFile> } BIND (</callimachus/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Image> } BIND (</callimachus/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/AnimatedGraphic> } BIND (</callimachus/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/IconGraphic> } BIND (</callimachus/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/NetworkGraphic> } BIND (</callimachus/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/VectorGraphic> } BIND (</callimachus/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Origin> } BIND (</callimachus/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Page> } BIND (</callimachus/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Pdf> } BIND (</callimachus/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Photo> } BIND (</callimachus/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Pipeline> } BIND (</callimachus/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Profile> } BIND (</callimachus/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/PURL> } BIND (</callimachus/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/NamedQuery> } BIND (</callimachus/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Realm> } BIND (</callimachus/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Relax> } BIND (</callimachus/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Schematron> } BIND (</callimachus/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Script> } BIND (</callimachus/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Serviceable> } BIND (</callimachus/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/SparqlService> } BIND (</callimachus/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Style> } BIND (</callimachus/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/TextFile> } BIND (</callimachus/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Transform> } BIND (</callimachus/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/User> } BIND (</callimachus/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/Viewable> } BIND (</callimachus/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/XQuery> } BIND (</callimachus/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?subclass),str(</callimachus/>)))
FILTER (!strstarts(str(?graph),str(</callimachus/>)))
};

DELETE {
	?resource a ?previous .
} INSERT {
	?resource a ?current .
} WHERE {
{ ?resource a </callimachus/types/Activity> BIND (</callimachus/types/Activity> AS ?previous) BIND (</callimachus/1.0/types/Change> AS ?current) } UNION
{ ?resource a </callimachus/types/Article> BIND (</callimachus/types/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ ?resource a </callimachus/types/Book> BIND (</callimachus/types/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ ?resource a </callimachus/types/Class> BIND (</callimachus/types/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ ?resource a </callimachus/types/Composite> BIND (</callimachus/types/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ ?resource a </callimachus/types/Concept> BIND (</callimachus/types/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ ?resource a </callimachus/types/Creatable> BIND (</callimachus/types/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ ?resource a </callimachus/types/DigestManager> BIND (</callimachus/types/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ ?resource a </callimachus/types/Editable> BIND (</callimachus/types/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ ?resource a </callimachus/types/FacebookManager> BIND (</callimachus/types/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ ?resource a </callimachus/types/File> BIND (</callimachus/types/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ ?resource a </callimachus/types/Folder> BIND (</callimachus/types/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ ?resource a </callimachus/types/Font> BIND (</callimachus/types/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ ?resource a </callimachus/types/SchemaGraph> BIND (</callimachus/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ ?resource a </callimachus/types/NamedGraph> BIND (</callimachus/types/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ ?resource a </callimachus/types/GraphDocument> BIND (</callimachus/types/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ ?resource a </callimachus/types/Group> BIND (</callimachus/types/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ ?resource a </callimachus/types/HypertextFile> BIND (</callimachus/types/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ ?resource a </callimachus/types/Image> BIND (</callimachus/types/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ ?resource a </callimachus/types/AnimatedGraphic> BIND (</callimachus/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ ?resource a </callimachus/types/IconGraphic> BIND (</callimachus/types/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ ?resource a </callimachus/types/NetworkGraphic> BIND (</callimachus/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ ?resource a </callimachus/types/VectorGraphic> BIND (</callimachus/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ ?resource a </callimachus/types/Origin> BIND (</callimachus/types/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ ?resource a </callimachus/types/Page> BIND (</callimachus/types/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ ?resource a </callimachus/types/Pdf> BIND (</callimachus/types/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ ?resource a </callimachus/types/Photo> BIND (</callimachus/types/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ ?resource a </callimachus/types/Pipeline> BIND (</callimachus/types/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ ?resource a </callimachus/types/Profile> BIND (</callimachus/types/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ ?resource a </callimachus/types/PURL> BIND (</callimachus/types/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ ?resource a </callimachus/types/NamedQuery> BIND (</callimachus/types/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ ?resource a </callimachus/types/Realm> BIND (</callimachus/types/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ ?resource a </callimachus/types/Relax> BIND (</callimachus/types/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ ?resource a </callimachus/types/Schematron> BIND (</callimachus/types/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ ?resource a </callimachus/types/Script> BIND (</callimachus/types/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ ?resource a </callimachus/types/Serviceable> BIND (</callimachus/types/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ ?resource a </callimachus/types/SparqlService> BIND (</callimachus/types/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ ?resource a </callimachus/types/Style> BIND (</callimachus/types/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ ?resource a </callimachus/types/TextFile> BIND (</callimachus/types/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ ?resource a </callimachus/types/Transform> BIND (</callimachus/types/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ ?resource a </callimachus/types/User> BIND (</callimachus/types/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ ?resource a </callimachus/types/Viewable> BIND (</callimachus/types/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ ?resource a </callimachus/types/XQuery> BIND (</callimachus/types/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?resource),str(</callimachus/>)))
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?previous }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?current }
} WHERE {
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Article> } BIND (</callimachus/types/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Book> } BIND (</callimachus/types/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Class> } BIND (</callimachus/types/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Composite> } BIND (</callimachus/types/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Concept> } BIND (</callimachus/types/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Creatable> } BIND (</callimachus/types/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/DigestManager> } BIND (</callimachus/types/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Editable> } BIND (</callimachus/types/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/FacebookManager> } BIND (</callimachus/types/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/File> } BIND (</callimachus/types/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Folder> } BIND (</callimachus/types/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Font> } BIND (</callimachus/types/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/SchemaGraph> } BIND (</callimachus/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/NamedGraph> } BIND (</callimachus/types/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/GraphDocument> } BIND (</callimachus/types/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Group> } BIND (</callimachus/types/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/HypertextFile> } BIND (</callimachus/types/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Image> } BIND (</callimachus/types/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/AnimatedGraphic> } BIND (</callimachus/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/IconGraphic> } BIND (</callimachus/types/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/NetworkGraphic> } BIND (</callimachus/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/VectorGraphic> } BIND (</callimachus/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Origin> } BIND (</callimachus/types/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Page> } BIND (</callimachus/types/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Pdf> } BIND (</callimachus/types/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Photo> } BIND (</callimachus/types/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Pipeline> } BIND (</callimachus/types/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Profile> } BIND (</callimachus/types/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/PURL> } BIND (</callimachus/types/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/NamedQuery> } BIND (</callimachus/types/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Realm> } BIND (</callimachus/types/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Relax> } BIND (</callimachus/types/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Schematron> } BIND (</callimachus/types/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Script> } BIND (</callimachus/types/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Serviceable> } BIND (</callimachus/types/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/SparqlService> } BIND (</callimachus/types/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Style> } BIND (</callimachus/types/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/TextFile> } BIND (</callimachus/types/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Transform> } BIND (</callimachus/types/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/User> } BIND (</callimachus/types/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/Viewable> } BIND (</callimachus/types/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/types/XQuery> } BIND (</callimachus/types/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?subclass),str(</callimachus/>)))
FILTER (!strstarts(str(?graph),str(</callimachus/>)))

};

DELETE {
	?resource a ?previous .
} INSERT {
	?resource a ?current .
} WHERE {
{ ?resource a </callimachus/0.18/types/Activity> BIND (</callimachus/0.18/types/Activity> AS ?previous) BIND (</callimachus/1.0/types/Change> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Change> BIND (</callimachus/0.18/types/Change> AS ?previous) BIND (</callimachus/1.0/types/Change> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Article> BIND (</callimachus/0.18/types/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Book> BIND (</callimachus/0.18/types/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Class> BIND (</callimachus/0.18/types/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Composite> BIND (</callimachus/0.18/types/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Concept> BIND (</callimachus/0.18/types/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Creatable> BIND (</callimachus/0.18/types/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/DigestManager> BIND (</callimachus/0.18/types/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Editable> BIND (</callimachus/0.18/types/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/FacebookManager> BIND (</callimachus/0.18/types/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/File> BIND (</callimachus/0.18/types/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Folder> BIND (</callimachus/0.18/types/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Font> BIND (</callimachus/0.18/types/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/SchemaGraph> BIND (</callimachus/0.18/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/NamedGraph> BIND (</callimachus/0.18/types/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/GraphDocument> BIND (</callimachus/0.18/types/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Group> BIND (</callimachus/0.18/types/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/HypertextFile> BIND (</callimachus/0.18/types/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Image> BIND (</callimachus/0.18/types/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/AnimatedGraphic> BIND (</callimachus/0.18/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/IconGraphic> BIND (</callimachus/0.18/types/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/NetworkGraphic> BIND (</callimachus/0.18/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/VectorGraphic> BIND (</callimachus/0.18/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Origin> BIND (</callimachus/0.18/types/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Page> BIND (</callimachus/0.18/types/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Pdf> BIND (</callimachus/0.18/types/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Photo> BIND (</callimachus/0.18/types/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Pipeline> BIND (</callimachus/0.18/types/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Profile> BIND (</callimachus/0.18/types/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/PURL> BIND (</callimachus/0.18/types/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/NamedQuery> BIND (</callimachus/0.18/types/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Realm> BIND (</callimachus/0.18/types/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Relax> BIND (</callimachus/0.18/types/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Schematron> BIND (</callimachus/0.18/types/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Script> BIND (</callimachus/0.18/types/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Serviceable> BIND (</callimachus/0.18/types/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/SparqlService> BIND (</callimachus/0.18/types/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Style> BIND (</callimachus/0.18/types/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/TextFile> BIND (</callimachus/0.18/types/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Transform> BIND (</callimachus/0.18/types/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/User> BIND (</callimachus/0.18/types/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/Viewable> BIND (</callimachus/0.18/types/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ ?resource a </callimachus/0.18/types/XQuery> BIND (</callimachus/0.18/types/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?resource),str(</callimachus/>)))
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?previous }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?current }
} WHERE {
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Article> } BIND (</callimachus/0.18/types/Article> AS ?previous) BIND (</callimachus/1.0/types/Article> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Book> } BIND (</callimachus/0.18/types/Book> AS ?previous) BIND (</callimachus/1.0/types/Book> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Class> } BIND (</callimachus/0.18/types/Class> AS ?previous) BIND (</callimachus/1.0/types/Class> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Composite> } BIND (</callimachus/0.18/types/Composite> AS ?previous) BIND (</callimachus/1.0/types/Composite> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Concept> } BIND (</callimachus/0.18/types/Concept> AS ?previous) BIND (</callimachus/1.0/types/Concept> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Creatable> } BIND (</callimachus/0.18/types/Creatable> AS ?previous) BIND (</callimachus/1.0/types/Creatable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/DigestManager> } BIND (</callimachus/0.18/types/DigestManager> AS ?previous) BIND (</callimachus/1.0/types/DigestManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Editable> } BIND (</callimachus/0.18/types/Editable> AS ?previous) BIND (</callimachus/1.0/types/Editable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/FacebookManager> } BIND (</callimachus/0.18/types/FacebookManager> AS ?previous) BIND (</callimachus/1.0/types/FacebookManager> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/File> } BIND (</callimachus/0.18/types/File> AS ?previous) BIND (</callimachus/1.0/types/File> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Folder> } BIND (</callimachus/0.18/types/Folder> AS ?previous) BIND (</callimachus/1.0/types/Folder> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Font> } BIND (</callimachus/0.18/types/Font> AS ?previous) BIND (</callimachus/1.0/types/Font> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/SchemaGraph> } BIND (</callimachus/0.18/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.0/types/SchemaGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/NamedGraph> } BIND (</callimachus/0.18/types/NamedGraph> AS ?previous) BIND (</callimachus/1.0/types/NamedGraph> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/GraphDocument> } BIND (</callimachus/0.18/types/GraphDocument> AS ?previous) BIND (</callimachus/1.0/types/GraphDocument> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Group> } BIND (</callimachus/0.18/types/Group> AS ?previous) BIND (</callimachus/1.0/types/Group> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/HypertextFile> } BIND (</callimachus/0.18/types/HypertextFile> AS ?previous) BIND (</callimachus/1.0/types/HypertextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Image> } BIND (</callimachus/0.18/types/Image> AS ?previous) BIND (</callimachus/1.0/types/Image> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/AnimatedGraphic> } BIND (</callimachus/0.18/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/IconGraphic> } BIND (</callimachus/0.18/types/IconGraphic> AS ?previous) BIND (</callimachus/1.0/types/IconGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/NetworkGraphic> } BIND (</callimachus/0.18/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.0/types/NetworkGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/VectorGraphic> } BIND (</callimachus/0.18/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.0/types/VectorGraphic> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Origin> } BIND (</callimachus/0.18/types/Origin> AS ?previous) BIND (</callimachus/1.0/types/Origin> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Page> } BIND (</callimachus/0.18/types/Page> AS ?previous) BIND (</callimachus/1.0/types/Page> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Pdf> } BIND (</callimachus/0.18/types/Pdf> AS ?previous) BIND (</callimachus/1.0/types/Pdf> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Photo> } BIND (</callimachus/0.18/types/Photo> AS ?previous) BIND (</callimachus/1.0/types/Photo> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Pipeline> } BIND (</callimachus/0.18/types/Pipeline> AS ?previous) BIND (</callimachus/1.0/types/Pipeline> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Profile> } BIND (</callimachus/0.18/types/Profile> AS ?previous) BIND (</callimachus/1.0/types/Profile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/PURL> } BIND (</callimachus/0.18/types/PURL> AS ?previous) BIND (</callimachus/1.0/types/PURL> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/NamedQuery> } BIND (</callimachus/0.18/types/NamedQuery> AS ?previous) BIND (</callimachus/1.0/types/NamedQuery> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Realm> } BIND (</callimachus/0.18/types/Realm> AS ?previous) BIND (</callimachus/1.0/types/Realm> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Relax> } BIND (</callimachus/0.18/types/Relax> AS ?previous) BIND (</callimachus/1.0/types/Relax> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Schematron> } BIND (</callimachus/0.18/types/Schematron> AS ?previous) BIND (</callimachus/1.0/types/Schematron> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Script> } BIND (</callimachus/0.18/types/Script> AS ?previous) BIND (</callimachus/1.0/types/Script> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Serviceable> } BIND (</callimachus/0.18/types/Serviceable> AS ?previous) BIND (</callimachus/1.0/types/Serviceable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/SparqlService> } BIND (</callimachus/0.18/types/SparqlService> AS ?previous) BIND (</callimachus/1.0/types/SparqlService> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Style> } BIND (</callimachus/0.18/types/Style> AS ?previous) BIND (</callimachus/1.0/types/Style> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/TextFile> } BIND (</callimachus/0.18/types/TextFile> AS ?previous) BIND (</callimachus/1.0/types/TextFile> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Transform> } BIND (</callimachus/0.18/types/Transform> AS ?previous) BIND (</callimachus/1.0/types/Transform> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/User> } BIND (</callimachus/0.18/types/User> AS ?previous) BIND (</callimachus/1.0/types/User> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/Viewable> } BIND (</callimachus/0.18/types/Viewable> AS ?previous) BIND (</callimachus/1.0/types/Viewable> AS ?current) } UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/0.18/types/XQuery> } BIND (</callimachus/0.18/types/XQuery> AS ?previous) BIND (</callimachus/1.0/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?subclass),str(</callimachus/0.18/>)))
FILTER (!strstarts(str(?graph),str(</callimachus/0.18/>)))
};

