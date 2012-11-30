# callimachus-webapp.ru
#
# read by Setup.java to determine initial Callimachus webapp path
# @webapp </callimachus/0.18/>
#
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

################################################################
# Data to initialize an Callimachus store, but may be removed.
################################################################

################################
# Version Info
################################

INSERT {
<../> calli:hasComponent <../ontology>.
<../ontology> a <types/Serviceable>, owl:Ontology;
    rdfs:label "ontology";
    rdfs:comment "Vocabulary used to create local Callimachus applications";
    owl:versionInfo "0.18";
    calli:administrator </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { <../ontology> a owl:Ontology }
};

################################
# Stable URLs
################################

INSERT {
<../../> calli:hasComponent <../>.
} WHERE {
	FILTER (<../../> != <../>)
	FILTER NOT EXISTS { <../../> calli:hasComponent <../> }
};

INSERT {
<../> a <types/Folder>, calli:Folder;
    rdfs:label "callimachus";
    calli:reader </auth/groups/public>;
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent
        <../changes/>,
        <../template.xsl>,
        <../library.xpl>,
        <../forbidden>,
        <../unauthorized> .

<../changes/> a <types/Folder>, calli:Folder;
    rdfs:label "changes";
    calli:subscriber </auth/groups/admin>.

<../template.xsl> a <types/PURL>, calli:PURL ;
	rdfs:label "template.xsl";
	calli:alternate <template.xsl>;
	calli:reader </auth/groups/public> ;
	calli:administrator </auth/groups/admin> .

<../library.xpl> a <types/PURL>, calli:PURL ;
	rdfs:label "library.xpl";
	calli:alternate <library.xpl>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .

<../forbidden> a <types/PURL>, calli:PURL ;
	rdfs:label "forbidden";
	calli:alternate <pages/forbidden.xhtml?element=/1&realm=/>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .

<../unauthorized> a <types/PURL>, calli:PURL ;
	rdfs:label "unauthorized";
	calli:alternate <pages/unauthorized.xhtml?element=/1>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .

<../Concept> a <types/PURL>, calli:PURL ;
	rdfs:label "Concept";
	calli:canonical <types/Concept>;
	calli:administrator </auth/groups/admin>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../> a calli:Folder }
};

################################
# Authorization and Groups
################################

INSERT {
</auth/> calli:hasComponent </auth/passwords/>.
</auth/passwords/> a <types/Folder>, calli:Folder;
    rdfs:label "passwords";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>.

</auth/> calli:hasComponent </auth/groups/>.
</auth/groups/> a <types/Folder>, calli:Folder;
    rdfs:label "groups";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent
        </auth/groups/admin>,
        </auth/groups/staff>,
        </auth/groups/users>,
        </auth/groups/everyone>,
        </auth/groups/system>,
        </auth/groups/public>.

</auth/groups/admin> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "admin";
    rdfs:comment "The user accounts in this group have heightened privileges, including the ability to edit other user accounts and access the underlying data store";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:membersFrom ".".

</auth/groups/staff> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "staff";
    rdfs:comment "Members of this group can design and edit resources within the system";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:membersFrom ".".

</auth/groups/users> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "users";
    rdfs:comment "Members of this group can view and discuss resources within the system";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:membersFrom ".".

</auth/groups/everyone> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "everyone";
    rdfs:comment "A virtual group of all authorized users";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:everyoneFrom ".".

</auth/groups/system> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "system";
    rdfs:comment "The local computer or computer systems is the member of this group";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>.

</auth/groups/public> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "public";
    rdfs:comment "A virtual group of all agents";
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:anonymousFrom ".".
} WHERE {
	FILTER NOT EXISTS { </auth/groups/> a calli:Folder }
};

################################
# Services
################################

INSERT {
</> calli:hasComponent </.well-known/>.
</.well-known/> a <types/Folder>, calli:Folder;
    rdfs:label ".well known";
    calli:reader </auth/groups/public>;
    calli:subscriber </auth/groups/staff>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent </.well-known/void>.

</.well-known/void> a <types/Serviceable>, void:DatasetDescription;
    rdfs:label "void";
    foaf:primaryTopic </.well-known/void#dataset>;
    calli:reader </auth/groups/public>.

</.well-known/void#dataset> a void:Dataset;
    foaf:homepage </>;
    void:sparqlEndpoint </sparql>;
    void:rootResource </>;
    void:openSearchDescription </?search>.

</> calli:hasComponent </sparql>.
</sparql> a <types/SparqlService>, sd:Service;
    rdfs:label "sparql";
    calli:administrator </auth/groups/admin>;
    sd:endpoint </sparql>;
    sd:supportedLanguage sd:SPARQL11Query, sd:SPARQL11Update;
    sd:feature sd:UnionDefaultGraph, sd:BasicFederatedQuery;
    sd:inputFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/Turtle>;
    sd:resultFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/SPARQL_Results_XML>.
} WHERE {
	FILTER NOT EXISTS { </> calli:hasComponent </.well-known/> }
};

INSERT {
	</.well-known/void#dataset> void:uriSpace ?realm
} WHERE {
	{
		?realm a <types/Realm>
		FILTER NOT EXISTS { ?origin calli:hasComponent ?realm }
	} UNION {
		?realm a <types/Origin>
	}
	FILTER NOT EXISTS { </.well-known/void#dataset> void:uriSpace [] }
};
