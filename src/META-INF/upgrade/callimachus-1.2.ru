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
<../> calli:hasComponent <../query-view.js>.
<../query-view.js> a <types/Purl>, calli:Purl ;
	rdfs:label "query-view.js";
	calli:alternate <scripts/query-view?minified>;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.js> }
};

DELETE {
<../query-view.js> calli:alternate <scripts/query-view.js>
} INSERT {
<../query-view.js> calli:alternate <scripts/query_bundle?minified>
} WHERE {
    <../query-view.js> calli:alternate <scripts/query-view.js>
};

DELETE {
<../query-view.js> calli:alternate <scripts/query-view?minified>
} INSERT {
<../query-view.js> calli:alternate <scripts/query_bundle?minified>
} WHERE {
<../query-view.js> calli:alternate <scripts/query-view?minified>
};

INSERT {
<../> calli:hasComponent <../query-view.css>.
<../query-view.css> a <types/Purl>, calli:Purl ;
	rdfs:label "query-view.css";
	calli:alternate <styles/callimachus-query-view.less?less>;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.css> }
};

DELETE {
    <queries/folder-create-menu.rq> calli:reader </auth/groups/system>, </auth/groups/admin>
} INSERT {
	<queries/folder-create-menu.rq> calli:reader </auth/groups/public>
} WHERE {
	FILTER NOT EXISTS { <queries/folder-create-menu.rq> calli:reader </auth/groups/public> }
};

DELETE {
	<../document-editor.html> calli:alternate ?oldalt
} INSERT {
	<../document-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/ckeditor.html>) AS ?oldalt)
    BIND (str(<pages/document-editor.html>) AS ?newalt)
	<../document-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../css-editor.html> calli:alternate ?oldalt
} INSERT {
	<../css-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#css>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#css>) AS ?newalt)
	<../css-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../html-editor.html> calli:alternate ?oldalt
} INSERT {
	<../html-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#html>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#html>) AS ?newalt)
	<../html-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../javascript-editor.html> calli:alternate ?oldalt
} INSERT {
	<../javascript-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#javascript>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#javascript>) AS ?newalt)
	<../javascript-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../sparql-editor.html> calli:alternate ?oldalt
} INSERT {
	<../sparql-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#sparql>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#sparql>) AS ?newalt)
	<../sparql-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../text-editor.html> calli:alternate ?oldalt
} INSERT {
	<../text-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html>) AS ?oldalt)
    BIND (str(<pages/text-editor.html>) AS ?newalt)
	<../text-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../xml-editor.html> calli:alternate ?oldalt
} INSERT {
	<../xml-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#xml>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#xml>) AS ?newalt)
	<../xml-editor.html> calli:alternate ?oldalt
};

DELETE {
	<../xquery-editor.html> calli:alternate ?oldalt
} INSERT {
	<../xquery-editor.html> calli:alternate ?newalt
} WHERE {
    BIND (str(<editor/text-editor.html#xquery>) AS ?oldalt)
    BIND (str(<pages/text-editor.html#xquery>) AS ?newalt)
	<../xquery-editor.html> calli:alternate ?oldalt
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

DELETE {
	?resource a ?previous .
} INSERT {
	?resource a ?current .
} WHERE {{ ?resource a </callimachus/1.0/types/AnimatedGraphic> BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.3/types/AnimatedGraphic> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Article> BIND (</callimachus/1.0/types/Article> AS ?previous) BIND (</callimachus/1.3/types/Article> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Book> BIND (</callimachus/1.0/types/Book> AS ?previous) BIND (</callimachus/1.3/types/Book> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Change> BIND (</callimachus/1.0/types/Change> AS ?previous) BIND (</callimachus/1.3/types/Change> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Class> BIND (</callimachus/1.0/types/Class> AS ?previous) BIND (</callimachus/1.3/types/Class> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Composite> BIND (</callimachus/1.0/types/Composite> AS ?previous) BIND (</callimachus/1.3/types/Composite> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Concept> BIND (</callimachus/1.0/types/Concept> AS ?previous) BIND (</callimachus/1.3/types/Concept> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Creatable> BIND (</callimachus/1.0/types/Creatable> AS ?previous) BIND (</callimachus/1.3/types/Creatable> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Credential> BIND (</callimachus/1.0/types/Credential> AS ?previous) BIND (</callimachus/1.3/types/Credential> AS ?current) } UNION
{ ?resource a calli:Datasource BIND (calli:Datasource AS ?previous) BIND (calli:RdfDatasource AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Datasource> BIND (</callimachus/1.0/types/Datasource> AS ?previous) BIND (</callimachus/1.3/types/RdfDatasource> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/Datasource> BIND (</callimachus/1.3/types/Datasource> AS ?previous) BIND (</callimachus/1.3/types/RdfDatasource> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/DigestManager> BIND (</callimachus/1.0/types/DigestManager> AS ?previous) BIND (</callimachus/1.3/types/DigestManager> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/DigestUser> BIND (</callimachus/1.0/types/DigestUser> AS ?previous) BIND (</callimachus/1.3/types/DigestUser> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Domain> BIND (</callimachus/1.0/types/Domain> AS ?previous) BIND (</callimachus/1.3/types/Domain> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Editable> BIND (</callimachus/1.0/types/Editable> AS ?previous) BIND (</callimachus/1.3/types/Editable> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/File> BIND (</callimachus/1.0/types/File> AS ?previous) BIND (</callimachus/1.3/types/File> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Folder> BIND (</callimachus/1.0/types/Folder> AS ?previous) BIND (</callimachus/1.3/types/Folder> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Font> BIND (</callimachus/1.0/types/Font> AS ?previous) BIND (</callimachus/1.3/types/Font> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/GraphDocument> BIND (</callimachus/1.0/types/GraphDocument> AS ?previous) BIND (</callimachus/1.3/types/RdfTurtle> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/GraphDocument> BIND (</callimachus/1.3/types/GraphDocument> AS ?previous) BIND (</callimachus/1.3/types/RdfTurtle> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Group> BIND (</callimachus/1.0/types/Group> AS ?previous) BIND (</callimachus/1.3/types/Group> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/HypertextFile> BIND (</callimachus/1.0/types/HypertextFile> AS ?previous) BIND (</callimachus/1.3/types/HypertextFile> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/IconGraphic> BIND (</callimachus/1.0/types/IconGraphic> AS ?previous) BIND (</callimachus/1.3/types/IconGraphic> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Image> BIND (</callimachus/1.0/types/Image> AS ?previous) BIND (</callimachus/1.3/types/Image> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/InvitedUser> BIND (</callimachus/1.0/types/InvitedUser> AS ?previous) BIND (</callimachus/1.3/types/InvitedUser> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/NamedGraph> BIND (</callimachus/1.0/types/NamedGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfGraph> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/NamedGraph> BIND (</callimachus/1.3/types/NamedGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfGraph> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/NamedQuery> BIND (</callimachus/1.0/types/NamedQuery> AS ?previous) BIND (</callimachus/1.3/types/RdfQuery> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/NamedQuery> BIND (</callimachus/1.3/types/NamedQuery> AS ?previous) BIND (</callimachus/1.3/types/RdfQuery> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/NetworkGraphic> BIND (</callimachus/1.0/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.3/types/NetworkGraphic> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/OpenIDManager> BIND (</callimachus/1.0/types/OpenIDManager> AS ?previous) BIND (</callimachus/1.3/types/OpenIDManager> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Origin> BIND (</callimachus/1.0/types/Origin> AS ?previous) BIND (</callimachus/1.3/types/Origin> AS ?current) } UNION
{ ?resource a calli:PURL BIND (calli:PURL AS ?previous) BIND (calli:Purl AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/PURL> BIND (</callimachus/1.0/types/PURL> AS ?previous) BIND (</callimachus/1.3/types/Purl> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/PURL> BIND (</callimachus/1.3/types/PURL> AS ?previous) BIND (</callimachus/1.3/types/Purl> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Page> BIND (</callimachus/1.0/types/Page> AS ?previous) BIND (</callimachus/1.3/types/Page> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Photo> BIND (</callimachus/1.0/types/Photo> AS ?previous) BIND (</callimachus/1.3/types/Photo> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Pipeline> BIND (</callimachus/1.0/types/Pipeline> AS ?previous) BIND (</callimachus/1.3/types/Pipeline> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Profile> BIND (</callimachus/1.0/types/Profile> AS ?previous) BIND (</callimachus/1.3/types/RdfProfile> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/Profile> BIND (</callimachus/1.3/types/Profile> AS ?previous) BIND (</callimachus/1.3/types/RdfProfile> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Realm> BIND (</callimachus/1.0/types/Realm> AS ?previous) BIND (</callimachus/1.3/types/Realm> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Relax> BIND (</callimachus/1.0/types/Relax> AS ?previous) BIND (</callimachus/1.3/types/Relax> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/SchemaGraph> BIND (</callimachus/1.0/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfSchemaGraph> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/SchemaGraph> BIND (</callimachus/1.3/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfSchemaGraph> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Schematron> BIND (</callimachus/1.0/types/Schematron> AS ?previous) BIND (</callimachus/1.3/types/Schematron> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Script> BIND (</callimachus/1.0/types/Script> AS ?previous) BIND (</callimachus/1.3/types/Script> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Serviceable> BIND (</callimachus/1.0/types/Serviceable> AS ?previous) BIND (</callimachus/1.3/types/Serviceable> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Style> BIND (</callimachus/1.0/types/Style> AS ?previous) BIND (</callimachus/1.3/types/StyleSheet> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/Style> BIND (</callimachus/1.3/types/Style> AS ?previous) BIND (</callimachus/1.3/types/StyleSheet> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/TextFile> BIND (</callimachus/1.0/types/TextFile> AS ?previous) BIND (</callimachus/1.3/types/TextFile> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Transform> BIND (</callimachus/1.0/types/Transform> AS ?previous) BIND (</callimachus/1.3/types/XslTransform> AS ?current) } UNION
{ ?resource a </callimachus/1.3/types/Transform> BIND (</callimachus/1.3/types/Transform> AS ?previous) BIND (</callimachus/1.3/types/XslTransform> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/VectorGraphic> BIND (</callimachus/1.0/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.3/types/VectorGraphic> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/Viewable> BIND (</callimachus/1.0/types/Viewable> AS ?previous) BIND (</callimachus/1.3/types/Viewable> AS ?current) } UNION
{ ?resource a </callimachus/1.0/types/XQuery> BIND (</callimachus/1.0/types/XQuery> AS ?previous) BIND (</callimachus/1.3/types/XQuery> AS ?current) }
FILTER (!strstarts(str(?resource),str(</callimachus/1.0/>)))
};

DELETE {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?previous }
} INSERT {
	GRAPH ?graph { ?subclass rdfs:subClassOf ?current }
} WHERE {
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/AnimatedGraphic> BIND (</callimachus/1.0/types/AnimatedGraphic> AS ?previous) BIND (</callimachus/1.3/types/AnimatedGraphic> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Article> BIND (</callimachus/1.0/types/Article> AS ?previous) BIND (</callimachus/1.3/types/Article> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Book> BIND (</callimachus/1.0/types/Book> AS ?previous) BIND (</callimachus/1.3/types/Book> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Change> BIND (</callimachus/1.0/types/Change> AS ?previous) BIND (</callimachus/1.3/types/Change> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Class> BIND (</callimachus/1.0/types/Class> AS ?previous) BIND (</callimachus/1.3/types/Class> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Composite> BIND (</callimachus/1.0/types/Composite> AS ?previous) BIND (</callimachus/1.3/types/Composite> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Concept> BIND (</callimachus/1.0/types/Concept> AS ?previous) BIND (</callimachus/1.3/types/Concept> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Creatable> BIND (</callimachus/1.0/types/Creatable> AS ?previous) BIND (</callimachus/1.3/types/Creatable> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Credential> BIND (</callimachus/1.0/types/Credential> AS ?previous) BIND (</callimachus/1.3/types/Credential> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf calli:Datasource BIND (calli:Datasource AS ?previous) BIND (calli:RdfDatasource AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Datasource> BIND (</callimachus/1.0/types/Datasource> AS ?previous) BIND (</callimachus/1.3/types/RdfDatasource> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/Datasource> BIND (</callimachus/1.3/types/Datasource> AS ?previous) BIND (</callimachus/1.3/types/RdfDatasource> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/DigestManager> BIND (</callimachus/1.0/types/DigestManager> AS ?previous) BIND (</callimachus/1.3/types/DigestManager> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/DigestUser> BIND (</callimachus/1.0/types/DigestUser> AS ?previous) BIND (</callimachus/1.3/types/DigestUser> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Domain> BIND (</callimachus/1.0/types/Domain> AS ?previous) BIND (</callimachus/1.3/types/Domain> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Editable> BIND (</callimachus/1.0/types/Editable> AS ?previous) BIND (</callimachus/1.3/types/Editable> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/File> BIND (</callimachus/1.0/types/File> AS ?previous) BIND (</callimachus/1.3/types/File> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Folder> BIND (</callimachus/1.0/types/Folder> AS ?previous) BIND (</callimachus/1.3/types/Folder> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Font> BIND (</callimachus/1.0/types/Font> AS ?previous) BIND (</callimachus/1.3/types/Font> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/GraphDocument> BIND (</callimachus/1.0/types/GraphDocument> AS ?previous) BIND (</callimachus/1.3/types/RdfTurtle> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/GraphDocument> BIND (</callimachus/1.3/types/GraphDocument> AS ?previous) BIND (</callimachus/1.3/types/RdfTurtle> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Group> BIND (</callimachus/1.0/types/Group> AS ?previous) BIND (</callimachus/1.3/types/Group> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/HypertextFile> BIND (</callimachus/1.0/types/HypertextFile> AS ?previous) BIND (</callimachus/1.3/types/HypertextFile> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/IconGraphic> BIND (</callimachus/1.0/types/IconGraphic> AS ?previous) BIND (</callimachus/1.3/types/IconGraphic> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Image> BIND (</callimachus/1.0/types/Image> AS ?previous) BIND (</callimachus/1.3/types/Image> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/InvitedUser> BIND (</callimachus/1.0/types/InvitedUser> AS ?previous) BIND (</callimachus/1.3/types/InvitedUser> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/NamedGraph> BIND (</callimachus/1.0/types/NamedGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfGraph> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/NamedGraph> BIND (</callimachus/1.3/types/NamedGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfGraph> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/NamedQuery> BIND (</callimachus/1.0/types/NamedQuery> AS ?previous) BIND (</callimachus/1.3/types/RdfQuery> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/NamedQuery> BIND (</callimachus/1.3/types/NamedQuery> AS ?previous) BIND (</callimachus/1.3/types/RdfQuery> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/NetworkGraphic> BIND (</callimachus/1.0/types/NetworkGraphic> AS ?previous) BIND (</callimachus/1.3/types/NetworkGraphic> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/OpenIDManager> BIND (</callimachus/1.0/types/OpenIDManager> AS ?previous) BIND (</callimachus/1.3/types/OpenIDManager> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Origin> BIND (</callimachus/1.0/types/Origin> AS ?previous) BIND (</callimachus/1.3/types/Origin> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf calli:PURL BIND (calli:PURL AS ?previous) BIND (calli:Purl AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/PURL> BIND (</callimachus/1.0/types/PURL> AS ?previous) BIND (</callimachus/1.3/types/Purl> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/PURL> BIND (</callimachus/1.3/types/PURL> AS ?previous) BIND (</callimachus/1.3/types/Purl> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Page> BIND (</callimachus/1.0/types/Page> AS ?previous) BIND (</callimachus/1.3/types/Page> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Photo> BIND (</callimachus/1.0/types/Photo> AS ?previous) BIND (</callimachus/1.3/types/Photo> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Pipeline> BIND (</callimachus/1.0/types/Pipeline> AS ?previous) BIND (</callimachus/1.3/types/Pipeline> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Profile> BIND (</callimachus/1.0/types/Profile> AS ?previous) BIND (</callimachus/1.3/types/RdfProfile> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/Profile> BIND (</callimachus/1.3/types/Profile> AS ?previous) BIND (</callimachus/1.3/types/RdfProfile> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Realm> BIND (</callimachus/1.0/types/Realm> AS ?previous) BIND (</callimachus/1.3/types/Realm> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Relax> BIND (</callimachus/1.0/types/Relax> AS ?previous) BIND (</callimachus/1.3/types/Relax> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/SchemaGraph> BIND (</callimachus/1.0/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfSchemaGraph> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/SchemaGraph> BIND (</callimachus/1.3/types/SchemaGraph> AS ?previous) BIND (</callimachus/1.3/types/RdfSchemaGraph> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Schematron> BIND (</callimachus/1.0/types/Schematron> AS ?previous) BIND (</callimachus/1.3/types/Schematron> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Script> BIND (</callimachus/1.0/types/Script> AS ?previous) BIND (</callimachus/1.3/types/Script> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Serviceable> BIND (</callimachus/1.0/types/Serviceable> AS ?previous) BIND (</callimachus/1.3/types/Serviceable> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Style> BIND (</callimachus/1.0/types/Style> AS ?previous) BIND (</callimachus/1.3/types/StyleSheet> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/Style> BIND (</callimachus/1.3/types/Style> AS ?previous) BIND (</callimachus/1.3/types/StyleSheet> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/TextFile> BIND (</callimachus/1.0/types/TextFile> AS ?previous) BIND (</callimachus/1.3/types/TextFile> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Transform> BIND (</callimachus/1.0/types/Transform> AS ?previous) BIND (</callimachus/1.3/types/XslTransform> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.3/types/Transform> BIND (</callimachus/1.3/types/Transform> AS ?previous) BIND (</callimachus/1.3/types/XslTransform> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/VectorGraphic> BIND (</callimachus/1.0/types/VectorGraphic> AS ?previous) BIND (</callimachus/1.3/types/VectorGraphic> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/Viewable> BIND (</callimachus/1.0/types/Viewable> AS ?previous) BIND (</callimachus/1.3/types/Viewable> AS ?current) }}  UNION
{ GRAPH ?graph { ?subclass rdfs:subClassOf </callimachus/1.0/types/XQuery> BIND (</callimachus/1.0/types/XQuery> AS ?previous) BIND (</callimachus/1.3/types/XQuery> AS ?current) }} 
FILTER (!strstarts(str(?subclass),str(</callimachus/1.0/>)))
FILTER (!strstarts(str(?graph),str(</callimachus/1.0/>)))
};

DELETE {
    ?pdf ?p ?o .
    ?folder calli:hasComponent ?pdf
} WHERE {
    ?pdf a </callimachus/1.0/types/Pdf> .
    ?pdf ?p ?o .
    ?folder calli:hasComponent ?pdf
};

INSERT {
    ?file a foaf:Document
} WHERE {
    ?file a [rdfs:subClassOf <types/File>]
    FILTER NOT EXISTS { ?file a foaf:Document }
    FILTER NOT EXISTS { ?file a foaf:Image }
};

INSERT {
    ?zip a calli:ZipArchive
} WHERE {
    ?zip a <types/ZipArchive>
    FILTER NOT EXISTS { ?zip a calli:ZipArchive }
};

DELETE {
    <../getting-started-with-callimachus>  calli:alternate <http://callimachusproject.org/docs/1.2/getting-started-with-callimachus.docbook?view>
} INSERT {
    <../getting-started-with-callimachus>  calli:alternate <http://callimachusproject.org/docs/1.3/getting-started-with-callimachus.docbook?view>
} WHERE {
    <../getting-started-with-callimachus>  calli:alternate <http://callimachusproject.org/docs/1.2/getting-started-with-callimachus.docbook?view>
};

DELETE {
    <../callimachus-for-web-developers> calli:alternate <http://callimachusproject.org/docs/1.2/callimachus-for-web-developers.docbook?view>
} INSERT {
    <../callimachus-for-web-developers> calli:alternate <http://callimachusproject.org/docs/1.3/callimachus-for-web-developers.docbook?view>
} WHERE {
    <../callimachus-for-web-developers> calli:alternate <http://callimachusproject.org/docs/1.2/callimachus-for-web-developers.docbook?view>
};

DELETE {
    <../callimachus-reference> calli:alternate <http://callimachusproject.org/docs/1.2/callimachus-reference.docbook?view>
} INSERT {
    <../callimachus-reference> calli:alternate <http://callimachusproject.org/docs/1.3/callimachus-reference.docbook?view>
} WHERE {
    <../callimachus-reference> calli:alternate <http://callimachusproject.org/docs/1.2/callimachus-reference.docbook?view>
};

DELETE {
	</callimachus/ontology> owl:versionInfo "1.2"
} INSERT {
	</callimachus/ontology> owl:versionInfo "1.3"
} WHERE {
	</callimachus/ontology> owl:versionInfo "1.2"
};

