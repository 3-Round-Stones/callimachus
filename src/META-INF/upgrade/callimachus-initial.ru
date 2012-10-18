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
PREFIX :<#>

################################################################
# Data to initialize an Callimachus store, but may be removed.
################################################################

################################
# Version Info
################################

INSERT DATA {
</> calli:hasComponent </callimachus>.
</callimachus> a </callimachus/Serviceable>, owl:Ontology;
    rdfs:label "Callimachus";
    rdfs:comment "Vocabulary used to create local Callimachus applications";
    owl:versionInfo "0.17";
    calli:administrator </group/admin>.
};

################################
# Default Folders
################################

INSERT DATA {
</> calli:hasComponent </activity/>.
</activity/> a </callimachus/Folder>, calli:Folder;
    rdfs:label "activity";
    calli:subscriber </group/admin>.

</> calli:hasComponent </user/>.
</user/> a </callimachus/Folder>, calli:Folder;
    rdfs:label "user";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>.

</> calli:hasComponent </group/>.
</group/> a </callimachus/Folder>, calli:Folder;
    rdfs:label "group";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>.
};

################################
# Authorization and Groups
################################

INSERT DATA {
</group/> calli:hasComponent </group/admin>.
</group/admin> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "admin";
    rdfs:comment "The user accounts in this group have heightened privileges, including the ability to edit other user accounts and access the underlying data store";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:membersFrom ".".

</group/> calli:hasComponent </group/staff>.
</group/staff> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "staff";
    rdfs:comment "Members of this group can design and edit resources within the system";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:membersFrom ".".

</group/> calli:hasComponent </group/users>.
</group/users> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "users";
    rdfs:comment "Members of this group can view and discuss resources within the system";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:membersFrom ".".

</group/> calli:hasComponent </group/everyone>.
</group/everyone> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "everyone";
    rdfs:comment "A virtual group of all authorized users";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:everyoneFrom ".".

</group/> calli:hasComponent </group/system>.
</group/system> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "system";
    rdfs:comment "The local computer or computer systems is the member of this group";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>.

</group/> calli:hasComponent </group/public>.
</group/public> a calli:Party, calli:Group, </callimachus/Group>;
    rdfs:label "public";
    rdfs:comment "A virtual group of all agents";
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:anonymousFrom ".".
};
################################
# Services
################################

INSERT DATA {
</> calli:hasComponent </.well-known/>.
</.well-known/> a </callimachus/Folder>, calli:Folder;
    rdfs:label ".well known";
    calli:reader </group/public>;
    calli:subscriber </group/staff>;
    calli:administrator </group/admin>;
    calli:hasComponent </.well-known/void>.

</.well-known/void> a </callimachus/Servicable>, void:DatasetDescription;
    rdfs:label "void";
    foaf:primaryTopic </.well-known/void#dataset>;
    calli:reader </group/public>.

</.well-known/void#dataset> a void:Dataset;
    foaf:homepage </>;
    void:sparqlEndpoint </sparql>;
    void:rootResource </>;
    void:openSearchDescription </?search>;
    void:uriSpace </>.

</> calli:hasComponent </sparql>.
</sparql> a </callimachus/SparqlService>, sd:Service;
    rdfs:label "sparql";
    calli:administrator </group/admin>;
    sd:endpoint </sparql>;
    sd:supportedLanguage sd:SPARQL11Query, sd:SPARQL11Update;
    sd:feature sd:UnionDefaultGraph, sd:BasicFederatedQuery;
    sd:inputFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/Turtle>;
    sd:resultFormat <http://www.w3.org/ns/formats/RDF_XML>, <http://www.w3.org/ns/formats/SPARQL_Results_XML>.
};

################################
# Menu
################################

INSERT DATA {
</> calli:hasComponent </main+menu>.
</main+menu> a </callimachus/Menu>, calli:Menu;
    rdfs:label "main menu";
    calli:reader </group/public>;
    calli:subscriber </group/users>;
    calli:editor </group/staff>;
    calli:administrator </group/admin>;
    calli:link </>;
    calli:item </main+menu#site>;
    calli:item </main+menu#toolbox>.

</main+menu#site> calli:position 1; rdfs:label "Site";
    calli:item </main+menu#mainarticle>;
    calli:item </main+menu#homefolder>;
    calli:item </main+menu#sitemap>;
    calli:item </main+menu#recentchanges>.
</main+menu#toolbox> calli:position 10; rdfs:label "Toolbox";
    calli:item </main+menu#whatlinkshere>;
    calli:item </main+menu#relatedchanges>;
    calli:item </main+menu#introspectresource>;
    calli:item </main+menu#permissions>;
    calli:item </main+menu#printthispage>.

</main+menu#mainarticle>	calli:position 1; rdfs:label "Main article"; calli:link </main-article.docbook?view>.
</main+menu#homefolder>	calli:position 2; rdfs:label "Home folder"; calli:link </?view>.
</main+menu#sitemap>	calli:position 3; rdfs:label "Site map"; calli:link </main+menu>.
</main+menu#recentchanges>	calli:position 4; rdfs:label "Recent changes"; calli:link </?changes>.

</main+menu#whatlinkshere>	calli:position 1; rdfs:label "What links here"; calli:link <javascript:location='?whatlinkshere'>.
</main+menu#relatedchanges>	calli:position 2; rdfs:label "Related changes"; calli:link <javascript:location='?relatedchanges'>.
</main+menu#introspectresource>	calli:position 3; rdfs:label "Introspect resource"; calli:link <javascript:location='?introspect'>.
</main+menu#permissions>	calli:position 3; rdfs:label "Permissions"; calli:link <javascript:location='?permissions'>.
</main+menu#printthispage>	calli:position 4; rdfs:label "Print this page"; calli:link <javascript:print()>.
};
