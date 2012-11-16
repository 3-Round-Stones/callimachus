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
	?resource a ?previous .
	?subclass rdfs:subClassOf ?previous .
} INSERT {
	?resource a ?current .
	?subclass rdfs:subClassOf ?current .
} WHERE {
    { BIND ("Article" AS ?name) } UNION
    { BIND ("Book" AS ?name) } UNION
    { BIND ("Class" AS ?name) } UNION
    { BIND ("Composite" AS ?name) } UNION
    { BIND ("Concept" AS ?name) } UNION
    { BIND ("Creatable" AS ?name) } UNION
    { BIND ("DigestManager" AS ?name) } UNION
    { BIND ("Editable" AS ?name) } UNION
    { BIND ("FacebookManager" AS ?name) } UNION
    { BIND ("File" AS ?name) } UNION
    { BIND ("Folder" AS ?name) } UNION
    { BIND ("Font" AS ?name) } UNION
    { BIND ("SchemaGraph" AS ?name) } UNION
    { BIND ("NamedGraph" AS ?name) } UNION
    { BIND ("GraphDocument" AS ?name) } UNION
    { BIND ("Group" AS ?name) } UNION
    { BIND ("HypertextFile" AS ?name) } UNION
    { BIND ("Image" AS ?name) } UNION
    { BIND ("AnimatedGraphic" AS ?name) } UNION
    { BIND ("IconGraphic" AS ?name) } UNION
    { BIND ("NetworkGraphic" AS ?name) } UNION
    { BIND ("VectorGraphic" AS ?name) } UNION
    { BIND ("Menu" AS ?name) } UNION
    { BIND ("Origin" AS ?name) } UNION
    { BIND ("Page" AS ?name) } UNION
    { BIND ("Pdf" AS ?name) } UNION
    { BIND ("Photo" AS ?name) } UNION
    { BIND ("Pipeline" AS ?name) } UNION
    { BIND ("Profile" AS ?name) } UNION
    { BIND ("PURL" AS ?name) } UNION
    { BIND ("NamedQuery" AS ?name) } UNION
    { BIND ("Realm" AS ?name) } UNION
    { BIND ("Relax" AS ?name) } UNION
    { BIND ("Schematron" AS ?name) } UNION
    { BIND ("Script" AS ?name) } UNION
    { BIND ("Serviceable" AS ?name) } UNION
    { BIND ("SparqlService" AS ?name) } UNION
    { BIND ("Style" AS ?name) } UNION
    { BIND ("TextFile" AS ?name) } UNION
    { BIND ("Theme" AS ?name) } UNION
    { BIND ("Transform" AS ?name) } UNION
    { BIND ("User" AS ?name) } UNION
    { BIND ("Viewable" AS ?name) } UNION
    { BIND ("XQuery" AS ?name) }
	BIND (iri(concat(str(</callimachus/>),?name)) AS ?previous)
	BIND (iri(concat(str(</callimachus/types/>),?name)) AS ?current)
	{
		?resource a ?previous
	} UNION {
		?subclass rdfs:subClassOf ?previous
	}
};
