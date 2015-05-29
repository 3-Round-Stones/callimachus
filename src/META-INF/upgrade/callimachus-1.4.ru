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
<../> calli:hasComponent <../markdown-editor.html> .
<../markdown-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "markdown-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#markdown>) AS ?alternate)
	FILTER NOT EXISTS { <../markdown-editor.html> a calli:Purl }
};

DELETE {
    <../> a calli:Folder
} INSERT {
    <../> a calli:PathSegment
} WHERE {
    <../> a calli:Folder
};

# Setup process determins the Callimachus webapp location based on Origin path
DELETE {
	</> a <../1.4/types/Origin>
} INSERT {
	</> a <../1.5/types/Origin>
} WHERE {
	</> a <../1.4/types/Origin>
};

DELETE {
    </admin> calli:alternate ?target
} INSERT {
    </admin> calli:alternate ?insertTarget
} WHERE {
    </admin> calli:alternate ?target
    FILTER (str(?target) = str(<../1.4/pages/invite-users.xhtml?view>))
    BIND (str(<../1.5/pages/invite-users.xhtml?view>) AS ?insertTarget)
};

DELETE {
?dataset void:uriLookupEndpoint </sparql?uri=>.
} INSERT {
?void calli:subscriber </auth/groups/power>, </auth/groups/admin>.
?dataset void:uriLookupEndpoint </sparql?resource=>.
} WHERE {
    BIND (iri(concat(str($origin),".well-known/void")) AS ?void)
    BIND (iri(concat(str($origin),".well-known/void#dataset")) AS ?dataset)
    ?void foaf:primaryTopic ?dataset.
    ?dataset a void:Dataset;
        void:sparqlEndpoint </sparql>;
        void:uriLookupEndpoint </sparql?uri=>.
};

# Setup process determins upgrade file based on versionInfo
DELETE {
	</callimachus/ontology> owl:versionInfo "1.4"
} INSERT {
	</callimachus/ontology> owl:versionInfo "1.4"
} WHERE {
	</callimachus/ontology> owl:versionInfo "1.4"
};

