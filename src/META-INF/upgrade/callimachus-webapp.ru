# callimachus-webapp.ru
#
# read by Setup.java to determine initial Callimachus webapp path
# @webapp </callimachus/1.4/>
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
    owl:versionInfo "1.3";
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
    calli:administrator </auth/groups/super>;
    calli:hasComponent
        <../profile>,
        <../changes/>,
        <../scripts.js>,
        <../library.xpl>,
        <../error.xpl>,
        <../forbidden.html>,
        <../unauthorized.html>,
        <../layout-functions.xq>,
        <../default-layout.xq>,
        <../callimachus-powered.png>,
        <../document-editor.html>,
        <../css-editor.html>,
        <../html-editor.html>,
        <../javascript-editor.html>,
        <../sparql-editor.html>,
        <../text-editor.html>,
        <../xml-editor.html>,
        <../xquery-editor.html>.
} WHERE {
	FILTER NOT EXISTS { <../> a calli:Folder }
};

INSERT {
<../profile> a <types/RdfProfile>;
    rdfs:label "rdf profile";
    calli:administrator </auth/groups/super>;
    calli:editor </auth/groups/power>,</auth/groups/admin>;
    calli:subscriber </auth/groups/staff>;
    calli:reader </auth/groups/system>.
} WHERE {
	FILTER NOT EXISTS { <../profile> a <types/RdfProfile> }
};

INSERT {
<../changes/> a <types/Folder>, calli:Folder;
    rdfs:label "changes";
    calli:subscriber </auth/groups/power>,</auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { <../changes/> a calli:Folder }
};

INSERT {
    </> calli:hasComponent </admin> .
    </admin> a <types/Purl>, calli:Purl ;
    rdfs:label "admin";
    rdfs:comment "Use the link below to manage groups and invite new users";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/admin> .
} WHERE {
    BIND (str(<pages/invite-users.xhtml?view>) AS ?alternate)
	FILTER NOT EXISTS { </admin> a calli:Purl }
};

INSERT {
    <../> calli:hasComponent <../getting-started-with-callimachus> .
    <../getting-started-with-callimachus> a <types/Purl>, calli:Purl ;
	rdfs:label "getting-started-with-callimachus";
	calli:alternate "http://callimachusproject.org/docs/1.4/getting-started-with-callimachus.docbook?view";
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../getting-started-with-callimachus> a calli:Purl }
};

INSERT {
    <../> calli:hasComponent <../callimachus-for-web-developers> .
    <../callimachus-for-web-developers> a <types/Purl>, calli:Purl ;
	rdfs:label "callimachus-for-web-developers";
	calli:alternate "http://callimachusproject.org/docs/1.4/callimachus-for-web-developers.docbook?view";
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../callimachus-for-web-developers> a calli:Purl }
};

INSERT {
    <../> calli:hasComponent <../callimachus-for-administrators> .
    <../callimachus-for-administrators> a <types/Purl>, calli:Purl ;
	rdfs:label "callimachus-for-administrators";
	calli:alternate "http://callimachusproject.org/docs/1.4/callimachus-for-administrators.docbook?view";
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../callimachus-for-administrators> a calli:Purl }
};

INSERT {
    <../> calli:hasComponent <../callimachus-reference> .
    <../callimachus-reference> a <types/Purl>, calli:Purl ;
	rdfs:label "callimachus-reference";
	calli:alternate "http://callimachusproject.org/docs/1.4/callimachus-reference.docbook?view";
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../callimachus-reference> a calli:Purl }
};

INSERT {
    <../> calli:hasComponent <../callimachus-for-administrators> .
    <../callimachus-for-administrators> a <types/Purl>, calli:Purl ;
	rdfs:label "callimachus-for-administrators";
	calli:alternate "http://callimachusproject.org/docs/1.4/callimachus-for-administrators.docbook?view";
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
	FILTER NOT EXISTS { <../callimachus-for-administrators> a calli:Purl }
};

INSERT {
<../scripts.js> a <types/Purl>, calli:Purl ;
	rdfs:label "scripts.js";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<scripts/index?minified>) AS ?alternate)
	FILTER NOT EXISTS { <../scripts.js> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../query-view.js>.
<../query-view.js> a <types/Purl>, calli:Purl ;
	rdfs:label "query-view.js";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<scripts/query_bundle?minified>) AS ?alternate)
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.js> }
};

INSERT {
<../> calli:hasComponent <../query-view.css>.
<../query-view.css> a <types/Purl>, calli:Purl ;
	rdfs:label "query-view.css";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<styles/callimachus-query-view.less?less>) AS ?alternate)
    FILTER NOT EXISTS { <../> calli:hasComponent <../query-view.css> }
};

INSERT {
<../library.xpl> a <types/Purl>, calli:Purl ;
	rdfs:label "library.xpl";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pipelines/library.xpl>) as ?alternate)
	FILTER NOT EXISTS { <../library.xpl> a calli:Purl }
};

INSERT {
<../error.xpl> a <types/Purl>, calli:Purl ;
	rdfs:label "error.xpl";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pipelines/error.xpl>) as ?alternate)
	FILTER NOT EXISTS { <../error.xpl> a calli:Purl }
};

INSERT {
<../forbidden.html> a <types/Purl>, calli:Purl ;
	rdfs:label "forbidden.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/forbidden.xhtml?html>) AS ?alternate)
	FILTER NOT EXISTS { <../forbidden.html> a calli:Purl }
};

INSERT {
<../unauthorized.html> a <types/Purl>, calli:Purl ;
	rdfs:label "unauthorized.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/unauthorized.xhtml?element=/1>) AS ?alternate)
	FILTER NOT EXISTS { <../unauthorized.html> a calli:Purl }
};

INSERT {
<../layout-functions.xq> a <types/Purl>, calli:Purl ;
	rdfs:label "layout-functions.xq";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<transforms/layout-functions.xq>) AS ?alternate)
	FILTER NOT EXISTS { <../layout-functions.xq> a calli:Purl }
};

INSERT {
<../default-layout.xq> a <types/Purl>, calli:Purl ;
	rdfs:label "default-layout.xq";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<transforms/default-layout.xq>) AS ?alternate)
	FILTER NOT EXISTS { <../default-layout.xq> a calli:Purl }
};

INSERT {
<../callimachus-powered.png> a <types/Purl>, calli:Purl ;
	rdfs:label "callimachus-powered.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/callimachus-powered.png>) AS ?alternate)
	FILTER NOT EXISTS { <../callimachus-powered.png> a calli:Purl }
};

INSERT {
<../document-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "document-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/document-editor.html>) AS ?alternate)
	FILTER NOT EXISTS { <../document-editor.html> a calli:Purl }
};

INSERT {
<../css-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "css-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#css>) AS ?alternate)
	FILTER NOT EXISTS { <../css-editor.html> a calli:Purl }
};

INSERT {
<../html-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "html-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#html>) AS ?alternate)
	FILTER NOT EXISTS { <../html-editor.html> a calli:Purl }
};

INSERT {
<../javascript-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "javascript-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#javascript>) AS ?alternate)
	FILTER NOT EXISTS { <../javascript-editor.html> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../json-editor.html> .
<../json-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "json-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#json>) AS ?alternate)
	FILTER NOT EXISTS { <../json-editor.html> a calli:Purl }
};

INSERT {
<../sparql-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "sparql-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#sparql>) AS ?alternate)
	FILTER NOT EXISTS { <../sparql-editor.html> a calli:Purl }
};

INSERT {
<../text-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "text-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html>) AS ?alternate)
	FILTER NOT EXISTS { <../text-editor.html> a calli:Purl }
};

INSERT {
<../xml-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "xml-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#xml>) AS ?alternate)
	FILTER NOT EXISTS { <../xml-editor.html> a calli:Purl }
};

INSERT {
<../xquery-editor.html> a <types/Purl>, calli:Purl ;
	rdfs:label "xquery-editor.html";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<pages/text-editor.html#xquery>) AS ?alternate)
	FILTER NOT EXISTS { <../xquery-editor.html> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../digest_64.png> .
<../digest_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "digest_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/digest_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../digest_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../google_64.png> .
<../google_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "google_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/google_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../google_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../yahoo_64.png> .
<../yahoo_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "yahoo_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/yahoo_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../yahoo_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../facebook_64.png> .
<../facebook_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "facebook_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/facebook_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../facebook_64.png> a calli:Purl }
};

INSERT {
<../> calli:hasComponent <../openid_64.png> .
<../openid_64.png> a <types/Purl>, calli:Purl ;
	rdfs:label "openid_64.png";
	calli:alternate ?alternate;
	calli:administrator </auth/groups/super>;
	calli:reader </auth/groups/public> .
} WHERE {
    BIND (str(<images/openid_64.png>) AS ?alternate)
	FILTER NOT EXISTS { <../openid_64.png> a calli:Purl }
};

################################
# Authorization and Groups
################################

INSERT {
</auth/> calli:hasComponent </auth/invited-users/>.
</auth/invited-users/> a <types/Folder>, calli:Folder;
    rdfs:label "invited users";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/invited-users/> a calli:Folder }
};

INSERT {
</auth/> calli:hasComponent </auth/digest-users/>.
</auth/digest-users/> a <types/Folder>, calli:Folder;
    rdfs:label "digest users";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/digest-users/> a calli:Folder }
};

INSERT {
</auth/> calli:hasComponent </auth/secrets/>.
</auth/secrets/> a <types/Folder>, calli:Folder;
    rdfs:label "secrets";
    calli:editor </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/secrets/> a calli:Folder }
};

INSERT {
</auth/> calli:hasComponent </auth/credentials/>.
</auth/credentials/> a <types/Folder>, calli:Folder;
    rdfs:label "credentials";
    calli:editor </auth/groups/admin>.
} WHERE {
	FILTER NOT EXISTS { </auth/credentials/> a calli:Folder }
};

INSERT {
</auth/> calli:hasComponent </auth/groups/>.
</auth/groups/> a <types/Folder>, calli:Folder;
    rdfs:label "groups";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>;
    calli:hasComponent
        </auth/groups/super>,
        </auth/groups/admin>,
        </auth/groups/power>,
        </auth/groups/staff>,
        </auth/groups/users>,
        </auth/groups/everyone>,
        </auth/groups/system>,
        </auth/groups/public>.

</auth/groups/super> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "super";
    rdfs:comment "The user accounts in this group have heightened privileges to change or patch the system itself".

</auth/groups/admin> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "admin";
    rdfs:comment "Members of this group have the ability to edit other user accounts and access to modify the underlying data store";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.

</auth/groups/power> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "power";
    rdfs:comment "Members of this group can access all data in the underlying data store";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.

</auth/groups/staff> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "staff";
    rdfs:comment "Members of this group can design websites and develop applications";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.

</auth/groups/users> a calli:Party, calli:Group, <types/Group>;
    rdfs:label "users";
    rdfs:comment "Members of this group can view, discuss, document, link, and upload binary resources";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.

</auth/groups/everyone> a calli:Party, calli:Domain, <types/Domain>;
    rdfs:label "everyone";
    rdfs:comment "A virtual group of all authenticated agents";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>;
    calli:everyoneFrom ".".

</auth/groups/system> a calli:Party, calli:Domain, <types/Domain>;
    rdfs:label "system";
    rdfs:comment "The local computer or computer systems is the member of this domain";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>.

</auth/groups/public> a calli:Party, calli:Domain, <types/Domain>;
    rdfs:label "public";
    rdfs:comment "A virtual group of all agents";
    calli:subscriber </auth/groups/power>;
    calli:administrator </auth/groups/admin>;
    calli:anonymousFrom ".".
} WHERE {
	FILTER NOT EXISTS { </auth/groups/> a calli:Folder }
};
